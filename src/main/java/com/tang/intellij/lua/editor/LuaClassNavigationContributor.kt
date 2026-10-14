package com.tang.intellij.lua.editor

import com.intellij.navigation.ChooseByNameContributorEx
import com.intellij.navigation.NavigationItem
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.Processor
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.indexing.FindSymbolParameters
import com.intellij.util.indexing.IdFilter
import com.tang.intellij.lua.stubs.index.LuaClassIndex

class LuaClassNavigationContributor : ChooseByNameContributorEx {
    override fun processNames(processor: Processor<in String>,
                              scope: GlobalSearchScope,
                              filter: IdFilter?) {
        ContainerUtil.process(LuaClassIndex.instance.getAllKeys(scope.project), processor)
    }

    override fun processElementsWithName(name: String,
                                         processor: Processor<in NavigationItem>,
                                         parameters: FindSymbolParameters) {
        val project = parameters.project
        val searchScope = parameters.searchScope
        LuaClassIndex.instance.processAllKeys(project) {
            ContainerUtil.process(LuaClassIndex.instance.get(it, project, searchScope), processor)
        }
    }
}
