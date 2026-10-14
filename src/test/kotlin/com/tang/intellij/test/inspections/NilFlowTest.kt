package com.tang.intellij.test.inspections

import com.tang.intellij.lua.codeInsight.inspection.MatchFunctionSignatureInspection
import com.tang.intellij.lua.codeInsight.inspection.UndeclaredMemberInspection
import com.tang.intellij.lua.project.LuaSettings
import com.intellij.psi.util.PsiTreeUtil
import com.tang.intellij.lua.psi.LuaNameExpr
import com.tang.intellij.lua.search.PsiSearchContext

class NilFlowTest : LuaInspectionsTestBase(UndeclaredMemberInspection(), MatchFunctionSignatureInspection()) {
    override fun setUp() {
        super.setUp()
        LuaSettings.instance.isNilStrict = true
    }

    override fun tearDown() {
        try {
            LuaSettings.instance.isNilStrict = false
        } finally {
            super.tearDown()
        }
    }

    private val definitions = """
        ---@class Info
        ---@field loc number
        ---@field child Info|nil
        local Info = {}
        ---@return Info
        function Info:clone() return self end
        ---@param info Info|nil
        ---@return Info|nil
        local function optional(info) return info end
        ---@param info Info
        local function consume(info) end
    """.trimIndent()

    fun testShortCircuit() = checkByText(definitions + """

        ---@param info Info|nil
        local function location(info)
            local log = info and info.loc or -1
            local inverted = not info or info.loc
            local explicit = info ~= nil and info.loc
            local reversed = nil ~= info and info.loc
            return info and info.loc ~= 0 and info.loc or nil
        end
    """.trimIndent())

    fun testBranchesAndArguments() = checkByText(definitions + """

        ---@param info Info|nil
        local function location(info)
            if info then consume(info) end
            if info ~= nil then local loc = info.loc end
            if info == nil then
                return nil
            elseif info.loc == 0 then
                return 0
            else
                return info.loc
            end
        end
        ---@param info Info|nil
        local function inverse(info)
            if not info then return nil else return info.loc end
        end
        ---@param info Info|nil
        local function loop(info)
            while info do
                local loc = info.loc
                info = nil
            end
        end
    """.trimIndent())

    fun testAssignedOptionalMember() = checkByText(definitions + """

        ---@param info Info
        local function update(info)
            info.child = info:clone()
            info.child.loc = 1
            local loc = info.child.loc
        end
    """.trimIndent())

    fun testAssignedSelfMember() = checkByText(definitions + """

        ---@class View
        ---@field original Info|nil
        ---@field fixed Info|nil
        local View = {}
        ---@param main Info
        ---@param loc number|nil
        function View:update(main, loc)
            if loc == nil then
                self.original = main
            else
                self.original = main
                self.fixed = main:clone()
                self.fixed.loc = loc
                local copy = self.fixed.loc
            end
        end
    """.trimIndent())

    private val nilLoc = """<error descr="No such member 'loc' found on type 'nil'">info.loc</error>"""
    private val nilChildLoc = """<error descr="No such member 'loc' found on type 'nil'">info.child.loc</error>"""

    fun testNoGuardLeaks() = checkByText(definitions + """

        ---@param info Info|nil
        local function location(info)
            local unguarded = $nilLoc
            local fallback = info or $nilLoc
            if info then local valid = info.loc end
            local outside = $nilLoc
            if info ~= false then local stillNullable = $nilLoc end
            local calledTwice = optional(info) and <error descr="No such member 'loc' found on type 'nil'">optional(info).loc</error>
        end
    """.trimIndent())

    fun testReassignmentAndCalls() = checkByText(definitions + """

        ---@param info Info|nil
        local function location(info)
            if info then
                local valid = info.loc
                info = nil
                local invalid = $nilLoc
            end
            if info then
                consume(info)
                local possiblyChanged = $nilLoc
            end
            local callBetween = info and consume(info) and $nilLoc
        end
    """.trimIndent())

    fun testShadowingAndClosure() = checkByText(definitions + """

        ---@param info Info|nil
        local function location(info)
            if info then
                do
                    ---@type Info|nil
                    local info
                    local invalid = $nilLoc
                end
                local later = function() return $nilLoc end
                local valid = info.loc
            end
        end
    """.trimIndent())

    fun testNullableWritesAndBranchMerge() = checkByText(definitions + """

        ---@param info Info
        ---@param flag boolean
        local function update(info, flag)
            info.child = nil
            local nilAssigned = $nilChildLoc
            info.child = optional(info)
            local nullableAssigned = $nilChildLoc
            if flag then info.child = info end
            local onlyOneBranch = $nilChildLoc
            info.child = info
            info.child = nil
            local reassigned = $nilChildLoc
        end
    """.trimIndent())

    fun testLoopBackEdge() = checkByText(definitions + """

        ---@param info Info|nil
        local function update(info)
            if info then
                while true do
                    local mayBeNilAfterFirstIteration = $nilLoc
                    info = nil
                end
            end
        end
        ---@return number
        local function bound() return 1 end
        ---@param info Info|nil
        local function numericLoop(info)
            if info then
                for i = 1, bound() do
                    local possiblyChangedByBound = $nilLoc
                end
            end
        end
    """.trimIndent())

    fun testFieldsAndParentheses() = checkByText(definitions + """

        ---@param info Info
        local function update(info)
            local loc = info.child and (info.child).loc
            local indexed = info["child"] and info["child"].loc
            local nested = (info.child ~= nil and info.child.loc ~= 0) and info.child.loc
            if info.child then
                info.child.loc = 1
                local afterDescendantWrite = info.child.loc
            end
        end
    """.trimIndent())

    fun testAncestorAndAliasWrites() = checkByText(definitions + """

        ---@param info Info
        ---@param other Info
        ---@param key string
        local function update(info, other, key)
            if info.child then
                info = other
                local invalid = $nilChildLoc
            end
            if info.child then
                <error descr="No such indexer '[string]' found on type 'Info'">info[key]</error> = nil
                local invalid = $nilChildLoc
            end
            if info.child then
                other.child = nil
                local possiblyAliased = $nilChildLoc
            end
        end
    """.trimIndent())

    fun testFalseIsDifferentFromNil() = checkByText(definitions + """

        ---@param info Info|nil|false
        local function location(info)
            local valid = info and info.loc
            local notNilMayBeFalse = info ~= nil and <error descr="No such member 'loc' found on type 'false'">info.loc</error>
            local notFalseMayBeNil = info ~= false and $nilLoc
            local validExplicit = info ~= nil and info ~= false and info.loc
        end
    """.trimIndent())

    fun testParallelAssignment() = checkByText(definitions + """

        ---@param info Info
        ---@param flag boolean
        local function update(info, flag)
            ---@type Info|nil
            local value
            value, flag = info, true
            local valid = value.loc
            flag, value = true, nil
            local invalid = <error descr="No such member 'loc' found on type 'nil'">value.loc</error>
            info.child, info = info, info:clone()
            local changedRoot = $nilChildLoc
        end
    """.trimIndent())

    fun testNarrowedTypeIsAvailableToOtherFeatures() {
        myFixture.configureByText("main.lua", definitions + """

            ---@alias OptionalInfo Info|nil
            ---@param info OptionalInfo
            local function location(info)
                return info and info<caret>.loc
            end
        """.trimIndent())
        val name = PsiTreeUtil.findElementOfClassAtOffset(myFixture.file, myFixture.caretOffset - 1,
            LuaNameExpr::class.java, false)!!
        val inferred = name.guessType(PsiSearchContext(name))!!
        assertEquals("Info", inferred.displayName)
    }
}
