/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.refactoring.move

import com.intellij.find.findUsages.FindUsagesHandler
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.refactoring.move.moveFilesOrDirectories.MoveFileHandler
import com.intellij.usageView.UsageInfo
import com.tang.intellij.lua.psi.LuaFileUtil
import com.tang.intellij.lua.psi.LuaPsiFile
import com.tang.intellij.lua.reference.LuaRequireReference
import java.util.*

class LuaMoveFileHandler : MoveFileHandler() {
    private class RequireUsageInfo(reference: LuaRequireReference, val targetFile: PsiFile) : UsageInfo(reference)

    override fun updateMovedFile(file: PsiFile) {

    }

    override fun prepareMovedFile(file: PsiFile, moveDestination: PsiDirectory, oldToNewMap: MutableMap<PsiElement, PsiElement>?) {

    }

    override fun findUsages(file: PsiFile, newParent: PsiDirectory, searchInComments: Boolean, searchInNonJavaFiles: Boolean): MutableList<UsageInfo> {
        val usages = mutableListOf<UsageInfo>()
        val handler = object : FindUsagesHandler(file){}
        val elementsToProcess = ArrayList<PsiElement>()
        Collections.addAll(elementsToProcess, *handler.primaryElements)
        Collections.addAll(elementsToProcess, *handler.secondaryElements)
        for (e in elementsToProcess) {
            handler.processElementUsages(e, { usageInfo ->
                if (!usageInfo.isNonCodeUsage) {
                    val reference = usageInfo.reference
                    usages.add(if (reference is LuaRequireReference) RequireUsageInfo(reference, file) else usageInfo)
                }
                true
            }, FindUsagesHandler.createFindUsagesOptions(file.project, null))
        }
        return usages
    }

    override fun retargetUsages(usageInfos: MutableList<out UsageInfo>, oldToNewMap: MutableMap<PsiElement, PsiElement>) {
        for (usageInfo in usageInfos) {
            val reference = usageInfo.reference
            if (reference is LuaRequireReference && usageInfo is RequireUsageInfo) {
                val file = oldToNewMap[usageInfo.targetFile] as? PsiFile ?: usageInfo.targetFile
                val requirePath = LuaFileUtil.asRequirePath(file.project, file.virtualFile)
                requirePath?.let {
                    reference.setPath(requirePath)
                }
            }
        }
    }

    override fun canProcessElement(file: PsiFile): Boolean {
        return file is LuaPsiFile
    }
}
