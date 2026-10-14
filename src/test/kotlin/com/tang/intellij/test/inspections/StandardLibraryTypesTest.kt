package com.tang.intellij.test.inspections

import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.codeInsight.inspection.AssignTypeInspection
import com.tang.intellij.lua.lang.LuaLanguageLevel
import com.tang.intellij.lua.project.LuaSettings
import com.tang.intellij.lua.project.StdLibraryProvider
import com.tang.intellij.lua.psi.LuaIndexExpr
import com.tang.intellij.lua.search.PsiSearchContext
import com.tang.intellij.lua.ty.Primitives

class StandardLibraryTypesTest : LuaInspectionsTestBase(AssignTypeInspection()) {
    private fun checkStringFields(level: LuaLanguageLevel, hasConfig: Boolean) {
        val settings = LuaSettings.instance
        val originalLevel = settings.languageLevel
        val originalStrictNil = settings.isNilStrict
        try {
            settings.languageLevel = level
            settings.isNilStrict = true
            StdLibraryProvider.reload()
            val fields = listOf("package.path", "package.cpath") +
                if (hasConfig) listOf("package.config") else emptyList()
            val validAssignments = fields.joinToString("\n") { "$it = directory .. \"?.lua\"" }
            val invalidAssignments = fields.joinToString("\n") {
                "$it = <error descr=\"Type mismatch. Required: 'string' Found: '42'\">42</error>"
            }
            checkByText("""
                ---@param directory string
                local function configure(directory)
                    $validAssignments
                end
                $invalidAssignments
            """.trimIndent())

            val accesses = PsiTreeUtil.findChildrenOfType(myFixture.file, LuaIndexExpr::class.java)
            for (field in fields) {
                val access = accesses.first { it.text == field }
                assertEquals(field, Primitives.STRING, access.guessType(PsiSearchContext(access)))
            }
        } finally {
            settings.languageLevel = originalLevel
            settings.isNilStrict = originalStrictNil
            StdLibraryProvider.reload()
        }
    }

    fun testLua51() = checkStringFields(LuaLanguageLevel.LUA51, false)
    fun testLua52() = checkStringFields(LuaLanguageLevel.LUA52, true)
    fun testLua53() = checkStringFields(LuaLanguageLevel.LUA53, true)
    fun testLua54() = checkStringFields(LuaLanguageLevel.LUA54, true)
}
