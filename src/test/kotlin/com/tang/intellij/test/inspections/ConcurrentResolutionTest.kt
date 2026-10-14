package com.tang.intellij.test.inspections

import com.intellij.openapi.application.ReadAction
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.LuaLocalDef
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.psi.resolve
import com.tang.intellij.lua.search.SearchContext
import com.tang.intellij.test.LuaTestBase
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class ConcurrentResolutionTest : LuaTestBase() {
    fun testLocalAssignmentResolvesDuringParallelInspections() {
        val file = myFixture.configureByText("parallel.lua", """
            ---@alias set<T> table<T, any>
            ---@type set<set<number>>
            local aSet
            aSet = aSet
        """.trimIndent())
        val declaration = PsiTreeUtil.findChildOfType(file, LuaLocalDef::class.java)!!
        val references = PsiTreeUtil.findChildrenOfType(file, LuaNameExpr::class.java)
        assertEquals(2, references.size)
        val start = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(6)
        try {
            val tasks = (1..6).map {
                executor.submit {
                    start.await()
                    ReadAction.run<RuntimeException> {
                        repeat(20) {
                            references.forEach { reference ->
                                assertSame(declaration, resolve(SearchContext.get(project), reference))
                            }
                        }
                    }
                }
            }
            start.countDown()
            tasks.forEach { it.get(10, TimeUnit.SECONDS) }
        } finally {
            executor.shutdownNow()
        }
    }
}
