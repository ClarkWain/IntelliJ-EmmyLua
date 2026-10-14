package com.tang.intellij.lua.ty

import com.intellij.openapi.progress.ProgressManager
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.*
import com.tang.intellij.lua.search.SearchContext

/** Facts at a reference's evaluation point, bounded by its function body.
 * Conditions are evaluated in order; writes and calls discard stale facts.
 * This deliberately does not propagate facts through aliases or merge branches.
 */
internal fun narrowFlowType(context: SearchContext, expression: LuaExpression<*>, type: ITy): ITy {
    if (context.isDumb || expression.assignStat != null ||
        (expression !is LuaNameExpr && expression !is LuaIndexExpr)) return type
    var canNarrow = false
    Ty.eachResolved(context, type) {
        if (it == Primitives.NIL || it == Primitives.FALSE || it == Primitives.BOOLEAN) canNarrow = true
    }
    if (!canNarrow) return type
    return FlowTypes(context, expression, type).infer()
}

private data class FlowSubject(val declaration: PsiElement, val name: String, val fields: List<String> = emptyList()) {
    fun contains(other: FlowSubject): Boolean = declaration == other.declaration && name == other.name &&
        fields.size <= other.fields.size && fields == other.fields.take(fields.size)
}

private class FlowTypes(
    private val context: SearchContext,
    private val expression: LuaExpression<*>,
    private val declaredType: ITy
) {
    private val tree = LuaDeclarationTree.get(expression.containingFile)
    private val subject = subject(expression)
    private var type = declaredType

    private fun unwrap(value: LuaExpression<*>): LuaExpression<*> =
        if (value is LuaParenExpr) value.expression?.let { unwrap(it) } ?: value else value

    private fun subject(value: LuaExpression<*>): FlowSubject? = when (val expr = unwrap(value)) {
        is LuaNameExpr -> {
            val name = expr.name
            val declaration = tree.find(expr)?.psi ?: if (name == "self") {
                PsiTreeUtil.getParentOfType(expr, LuaClassMethodDefStat::class.java) ?: expr.containingFile
            } else expr.containingFile
            FlowSubject(declaration, name)
        }
        is LuaIndexExpr -> {
            // PSI normalizes named members and literal string keys to the same name.
            // Numeric/dynamic indices need Lua key normalization and alias analysis.
            val field = expr.name
            if (field == null) null else subject(expr.prefixExpression)?.let { it.copy(fields = it.fields + field) }
        }
        else -> null
    }

    fun infer(): ITy {
        if (subject == null) return declaredType
        val path = mutableListOf<PsiElement>()
        var child: PsiElement = expression
        while (true) {
            val parent = child.parent ?: break
            if (parent is LuaFuncBodyOwner<*>) break
            path.add(child)
            child = parent
        }
        for (pin in path.asReversed()) {
            ProgressManager.checkCanceled()
            val parent = pin.parent
            when {
                parent is LuaIfStat -> branch(parent, pin)
                parent is LuaLoop && pin is LuaBlock -> {
                    // A previous iteration may have changed the value before reaching this one.
                    if (mayInvalidate(parent)) type = declaredType
                    if (parent is LuaWhileStat) {
                        parent.children.filterIsInstance<LuaExpression<*>>().firstOrNull()?.let { assume(it, true) }
                    }
                }
                parent is LuaBinaryExpr && pin == parent.right -> {
                    val left = parent.left ?: continue
                    when (parent.operationType) {
                        LuaTypes.AND -> assume(left, true)
                        LuaTypes.OR -> assume(left, false)
                        else -> effects(left)
                    }
                }
                else -> parent.children.takeWhile { it != pin }.forEach { effects(it) }
            }
        }
        return type
    }

    private fun branch(statement: LuaIfStat, pin: PsiElement) {
        var condition: LuaExpression<*>? = null
        for (child in statement.children) {
            if (child == pin) {
                if (child is LuaBlock) condition?.let { assume(it, true) }
                return
            }
            if (child is LuaExpression<*>) condition = child
            if (child is LuaBlock) {
                condition?.let { assume(it, false) }
                condition = null
            }
        }
    }

    private fun assume(value: LuaExpression<*>, truthy: Boolean) {
        val expr = unwrap(value)
        if (expr is LuaUnaryExpr && expr.unaryOp.node.firstChildNode.elementType == LuaTypes.NOT) {
            expr.expression?.let { assume(it, !truthy) }
            return
        }
        if (expr is LuaBinaryExpr) {
            val left = expr.left
            val right = expr.right
            when (expr.operationType) {
                LuaTypes.AND, LuaTypes.OR -> {
                    if ((expr.operationType == LuaTypes.AND) == truthy) {
                        left?.let { assume(it, truthy) }
                        right?.let { assume(it, truthy) }
                    } else effects(expr) // Either operand may have selected this branch.
                    return
                }
                LuaTypes.EQ, LuaTypes.NE -> {
                    effects(expr)
                    if (left != null && right != null) {
                        val literal = literal(right) ?: literal(left)
                        val tested = if (literal(right) != null) left else right
                        if (literal != null && subject(tested) == subject) {
                            filter { ty ->
                                if (ty.isUnknown || ty.isGlobal) ty
                                else if (ty == Primitives.BOOLEAN && literal != Primitives.NIL) {
                                    if ((expr.operationType == LuaTypes.EQ) == truthy) literal
                                    else if (literal == Primitives.FALSE) Primitives.TRUE else Primitives.FALSE
                                } else if ((ty == literal) == ((expr.operationType == LuaTypes.EQ) == truthy)) ty else null
                            }
                        }
                    }
                    return
                }
            }
        }
        effects(expr)
        if (subject(expr) == subject) {
            filter { ty ->
                when {
                    ty.isUnknown || ty.isGlobal -> ty
                    ty == Primitives.BOOLEAN -> if (truthy) Primitives.TRUE else Primitives.FALSE
                    (ty.booleanType != Primitives.FALSE) == truthy -> ty
                    else -> null
                }
            }
        }
    }

    private fun literal(value: LuaExpression<*>): ITy? = when (unwrap(value)) {
        is LuaLiteralExpr -> when (unwrap(value).text) {
            "nil" -> Primitives.NIL
            "false" -> Primitives.FALSE
            "true" -> Primitives.TRUE
            else -> null
        }
        else -> null
    }

    private fun filter(select: (ITy) -> ITy?) {
        val retained = mutableListOf<ITy>()
        Ty.eachUnresolved(context, type) { unresolved, resolved ->
            select(resolved)?.let { retained.add(if (it == resolved) unresolved else it) }
        }
        // Leave unreachable expressions conservative rather than inventing a value type.
        if (retained.isNotEmpty()) type = TyUnion.union(context, retained)
    }

    private fun effects(element: PsiElement) {
        ProgressManager.checkCanceled()
        if (element is LuaFuncBodyOwner<*>) return // A closure body is not executed at its declaration.
        if (element is LuaAssignStat) {
            element.varExprList.expressionList.forEach { effects(it) }
            element.valueExprList?.let { effects(it) }
            val targets = element.varExprList.expressionList
            // Evaluate all RHS values before applying writes (including parallel assignment).
            var assigned: ITy? = null
            var invalidated = false
            targets.forEachIndexed { index, target ->
                val written = subject(target)
                if (written != null && written.contains(subject!!)) {
                    invalidated = true
                    assigned = null
                    if (written == subject) {
                        assigned = context.withMultipleResults {
                            element.valueExprList?.guessType(context)?.let { TyMultipleResults.getResult(context, it, index) }
                        }
                    }
                } else if (invalidates(target, written)) {
                    invalidated = true
                    assigned = null
                }
            }
            if (invalidated) type = declaredType
            assigned?.let { rhs ->
                var nonNil = true
                var truthy = true
                Ty.eachResolved(context, rhs) {
                    if (it == Primitives.NIL || it.isUnknown || it.isGlobal || it == Primitives.VOID) nonNil = false
                    if (it.booleanType != Primitives.TRUE || it.isUnknown || it.isGlobal) truthy = false
                }
                if (nonNil) filter { ty ->
                    when {
                        ty == Primitives.NIL -> null
                        truthy && ty == Primitives.FALSE -> null
                        truthy && ty == Primitives.BOOLEAN -> Primitives.TRUE
                        else -> ty
                    }
                }
            }
            return
        }
        if (element is LuaIfStat || element is LuaLoop) {
            // Facts from one branch/iteration cannot be used after the statement.
            if (mayInvalidate(element)) type = declaredType
            return
        }
        element.children.forEach { effects(it) }
        if (element is LuaCallExpr) type = declaredType
    }

    private fun mayInvalidate(element: PsiElement): Boolean {
        if (element is LuaFuncBodyOwner<*>) return false
        if (element is LuaCallExpr) return true
        if (element is LuaAssignStat && element.varExprList.expressionList.any {
            subject(it)?.contains(subject!!) == true || invalidates(it, subject(it))
        }) return true
        return element.children.any { mayInvalidate(it) }
    }

    private fun invalidates(target: LuaExpression<*>, written: FlowSubject?): Boolean {
        if (target !is LuaIndexExpr) return false
        val queried = subject ?: return false
        if (written == null) return subject(target.prefixExpression)?.contains(queried) != false
        // An unrelated table may alias the queried table. Descendant/sibling writes on
        // the same root are safe, but writes through another root cannot be proven safe.
        return queried.fields.isNotEmpty() &&
            (written.declaration != queried.declaration || written.name != queried.name)
    }
}
