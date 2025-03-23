package com.github.paulknisely.intellijpromptcopyplugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CopySelectedFilesAction : BaseFileCopyAction("Copy Selected Files to Clipboard") {

    override fun getFilesToProcess(e: AnActionEvent, project: Project): List<VirtualFile> {
        val selectedFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: return emptyList()

        // Remove nested selections to avoid duplication
        return removeNestedSelections(selectedFiles.toList())
    }
}
