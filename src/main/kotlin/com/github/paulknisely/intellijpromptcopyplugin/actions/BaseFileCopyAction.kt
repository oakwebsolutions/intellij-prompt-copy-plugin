package com.github.paulknisely.intellijpromptcopyplugin.actions

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.ProjectLevelVcsManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileVisitor
import com.intellij.openapi.vfs.VfsUtilCore
import java.awt.Toolkit
import java.awt.datatransfer.StringSelection

/**
 * Base class for actions that copy file content to clipboard
 */
abstract class BaseFileCopyAction(text: String) : AnAction(text) {

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.getData(CommonDataKeys.PROJECT) ?: return

        // Get files to process (implementation varies by subclass)
        val filesToProcess = getFilesToProcess(e, project)
        if (filesToProcess.isEmpty()) return

        // Find repository roots
        val repoRoots = findRepositoryRoots(project)

        // Build the content string
        val contentBuilder = StringBuilder()

        // Add optional header (can be overridden by subclasses)
        addHeader(contentBuilder)

        // Process each file
        processFiles(filesToProcess, contentBuilder, repoRoots)

        // Copy to clipboard
        val clipboardContent = StringSelection(contentBuilder.toString())
        Toolkit.getDefaultToolkit().systemClipboard.setContents(clipboardContent, null)
    }

    /**
     * Get the files to process - must be implemented by subclasses
     */
    protected abstract fun getFilesToProcess(e: AnActionEvent, project: Project): List<VirtualFile>

    /**
     * Add a header to the content - can be overridden by subclasses
     */
    protected open fun addHeader(builder: StringBuilder) {
        // Default implementation does nothing
    }

    /**
     * Process the list of files and add their content to the builder
     */
    protected fun processFiles(files: List<VirtualFile>, builder: StringBuilder, repoRoots: List<VirtualFile>) {
        files.forEach { file ->
            when {
                file.isDirectory -> collectDirectoryContent(file, builder, repoRoots)
                else -> appendFileContent(file, builder, repoRoots)
            }
        }
    }

    /**
     * Adds file content to the builder with appropriate formatting
     */
    protected fun appendFileContent(file: VirtualFile, builder: StringBuilder, repoRoots: List<VirtualFile>) {
        // Skip non-text files
        if (file.fileType.isBinary) return

        try {
            // Get relative path from repository root
            val relativePath = getRelativePath(file, repoRoots)

            // Add file path as header
            builder.append("File: $relativePath\n")

            // Add code fence with file extension for syntax highlighting
            val extension = file.extension?.let { "```$it\n" } ?: "```\n"
            builder.append(extension)

            // Read and append file content
            val content = file.inputStream.reader(Charsets.UTF_8).use { it.readText() }
            builder.append(content)

            // Close code fence and add spacing
            builder.append("\n```\n\n")
        } catch (e: Exception) {
            builder.append("// Error reading ${file.path}: ${e.message}\n\n")
        }
    }

    /**
     * Collects content from all files in a directory
     */
    protected fun collectDirectoryContent(directory: VirtualFile, builder: StringBuilder, repoRoots: List<VirtualFile>) {
        VfsUtilCore.visitChildrenRecursively(directory, object : VirtualFileVisitor<Void>() {
            override fun visitFile(file: VirtualFile): Boolean {
                if (!file.isDirectory) {
                    appendFileContent(file, builder, repoRoots)
                }
                return true // continue visiting
            }
        })
    }

    /**
     * Finds all repository roots for the project
     */
    protected fun findRepositoryRoots(project: Project): List<VirtualFile> {
        val vcsManager = ProjectLevelVcsManager.getInstance(project)
        return vcsManager.allVcsRoots.mapNotNull { it.path }
    }

    /**
     * Gets path relative to repository root, or project root if no VCS root is found
     */
    protected fun getRelativePath(file: VirtualFile, repoRoots: List<VirtualFile>): String {
        // Find the most specific repository root that contains this file
        val containingRoot = repoRoots.filter { root ->
            isContainedIn(file, root)
        }.maxByOrNull { it.path.length } // Get the deepest matching root

        return if (containingRoot != null) {
            // Get path relative to repo root
            val rootPath = containingRoot.path
            val filePath = file.path
            if (filePath.startsWith(rootPath)) {
                filePath.substring(rootPath.length).removePrefix("/")
            } else {
                file.name // Fallback to just the filename
            }
        } else {
            // If no repo root contains this file, return the full path
            file.path
        }
    }

    /**
     * Determines if a file is contained within another directory
     */
    protected fun isContainedIn(file: VirtualFile, potentialParent: VirtualFile): Boolean {
        if (!potentialParent.isDirectory) return false

        var current: VirtualFile? = file
        while (current != null) {
            if (current == potentialParent) return true
            current = current.parent
        }

        return false
    }

    /**
     * Removes selections that are contained within other selected directories
     * to avoid duplicate content
     */
    protected fun removeNestedSelections(files: List<VirtualFile>): List<VirtualFile> {
        return files.filterNot { candidate ->
            files.any { other ->
                other != candidate && isContainedIn(candidate, other)
            }
        }
    }
}
