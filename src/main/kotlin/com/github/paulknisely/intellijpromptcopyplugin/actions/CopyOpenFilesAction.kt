package com.github.paulknisely.intellijpromptcopyplugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

class CopyOpenFilesAction : BaseFileCopyAction("Copy All Open Files to Clipboard") {

    override fun getFilesToProcess(e: AnActionEvent, project: Project): List<VirtualFile> {
        return FileEditorManager.getInstance(project).openFiles.toList()
    }

    override fun addHeader(builder: StringBuilder) {
        builder.append("# Open Files in Project\n\n")
    }
}
