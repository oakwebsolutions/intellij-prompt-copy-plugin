package com.github.paulknisely.intellijpromptcopyplugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CopyCurrentFileAction : BaseFileCopyAction("Copy Current File to Clipboard") {

    override fun getFilesToProcess(e: AnActionEvent, project: Project): List<VirtualFile> {
        // Get the file currently in focus
        val currentFile = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return emptyList()
        return listOf(currentFile)
    }

    override fun addHeader(builder: StringBuilder) {
        builder.append("# Current File\n\n")
    }
}
