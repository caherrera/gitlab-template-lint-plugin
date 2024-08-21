package com.github.caherrera.gitlab.template.lint.plugin.providers

import com.github.caherrera.gitlab.template.lint.plugin.GitlabLintUtils
import com.github.caherrera.gitlab.template.lint.plugin.settings.AppSettings
import com.intellij.openapi.Disposable
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorFactory
import com.intellij.openapi.fileEditor.*
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager

private const val GITLAB_LINT_EDITOR_PROVIDER: String = "GitlabLintEditorProvider"
class GitlabLintEditorProvider : FileEditorProvider, DumbAware {
    override fun getEditorTypeId() = GITLAB_LINT_EDITOR_PROVIDER

    override fun accept(project: Project, file: VirtualFile): Boolean {
        if (!file.isValid) return false
        PsiManager.getInstance(project).findFile(file) ?: return false
        return GitlabLintUtils.isGitlabYaml(file) && AppSettings.instance.showMergedPreview
    }

    override fun createEditor(project: Project, file: VirtualFile): FileEditor {
        return EditorWithMergedPreview.create(file, project)
    }

    override fun getPolicy(): FileEditorPolicy = FileEditorPolicy.HIDE_DEFAULT_EDITOR
}

class EditorWithMergedPreview private constructor(
    editor: TextEditor,
    private val preview: TextEditor,
    private val editorViewer: Editor
) : TextEditorWithPreview(editor, preview) {

    init {
        (editor as? Disposable)?.let { Disposer.register(this, it) }
        (preview as? Disposable)?.let { Disposer.register(this, it) }
    }

    override fun dispose() {
        EditorFactory.getInstance().releaseEditor(editorViewer)
    }

    fun setPreviewText(text: String) {
        preview.editor.document.setText(text)
    }
    companion object {
        fun create(file: VirtualFile, project: Project): EditorWithMergedPreview {
            val textEditorProvider = TextEditorProvider.getInstance()

            val mainEditor = textEditorProvider.createEditor(project, file) as TextEditor
            val editorFactory = EditorFactory.getInstance()

            val editorViewer = editorFactory.createEditor(
                editorFactory.createDocument(""),
                project,
                file.fileType,
                true
            )

            // Removes vertical line
            editorViewer.settings.isRightMarginShown = false

            val previewEditor = textEditorProvider.getTextEditor(editorViewer)

            return EditorWithMergedPreview(mainEditor, previewEditor, editorViewer)
        }
    }
}
