package com.github.caherrera.gitlab.template.lint.plugin.settings.gitlabUrlToken

import com.github.caherrera.gitlab.template.lint.plugin.GitlabLintBundle.message
import com.github.caherrera.gitlab.template.lint.plugin.extensions.createColumn
import com.github.caherrera.gitlab.template.lint.plugin.extensions.emptyText
import com.github.caherrera.gitlab.template.lint.plugin.extensions.notBlank
import com.github.caherrera.gitlab.template.lint.plugin.extensions.replaceAt
import com.github.caherrera.gitlab.template.lint.plugin.gitlab.Gitlab
import com.github.caherrera.gitlab.template.lint.plugin.settings.AppSettings
import com.github.caherrera.gitlab.template.lint.plugin.settings.ProjectSettings
import com.intellij.icons.AllIcons
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.runBackgroundableTask
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.openapi.diagnostic.Logger
import com.intellij.ui.components.JBTextField
import com.intellij.ui.dsl.builder.Align
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.table.TableView
import com.intellij.util.ui.ListTableModel
import java.awt.event.ActionEvent
import java.net.URL
import javax.swing.Action
import javax.swing.JLabel
import javax.swing.ListSelectionModel.SINGLE_SELECTION


class GitlabUrlTokenTable(val project: Project) {
    private var logger = Logger.getInstance(GitlabUrlTokenTable::class.java)
    private var gitlabUrls = project.service<ProjectSettings>().gitlabUrls
    private var gitlabUrlTokens = project.service<ProjectSettings>().gitlabUrlTokens

    private val tableModel = createTableModel()

    val table = TableView(tableModel).apply {
        setShowColumns(true)
        setSelectionMode(SINGLE_SELECTION)
        setDefaultRenderer(String::class.java, GitlabTokenRenderer(2))
    }

    private fun createTableModel(): ListTableModel<GitlabUrlToken> = ListTableModel(
        arrayOf(
            createColumn(message("settings.gitlab-url-token.gitlab-alias")) { gitlabUrlToken -> gitlabUrlToken.gitlabAlias },
            createColumn(message("settings.gitlab-url-token.gitlab-url")) { gitlabUrlToken -> gitlabUrlToken.gitlabUrl.orEmpty() },
            createColumn<GitlabUrlToken>(message("settings.gitlab-url-token.gitlab-token")) { gitlabUrlToken -> gitlabUrlToken.gitlabToken.orEmpty() }
        ),
        gitlabUrlTokens
    )

    fun addGitlabUrlToken() {
        val dialog = GitlabUrlTokenDialog(project)

        if (dialog.showAndGet()) {
            logger.info("Adding GitlabUrlToken: ${dialog.gitlabUrlToken}")
            gitlabUrlTokens = gitlabUrlTokens.plus(dialog.gitlabUrlToken)
            refreshTableModel()
        }
    }

    fun removeGitlabUrlToken() {
        val row = table.selectedObject ?: return
        gitlabUrlTokens = gitlabUrlTokens.minus(row)
        gitlabUrls = gitlabUrls.minus(row.gitlabAlias)
        refreshTableModel()
    }

    fun editGitlabUrlToken() {
        val row = table.selectedObject ?: return

        val dialog = GitlabUrlTokenDialog(project, row.copy())

        if (dialog.showAndGet()) {
            gitlabUrlTokens = gitlabUrlTokens.replaceAt(table.selectedRow, dialog.gitlabUrlToken)
            refreshTableModel()
        }
    }

    private fun refreshTableModel() {
        tableModel.items = gitlabUrlTokens
    }

    fun reset() {
        gitlabUrlTokens =
            gitlabUrlTokens.map { GitlabUrlToken(it.gitlabAlias, "",AppSettings.instance.getGitlabTokenBlocking(it.gitlabAlias)) }
        refreshTableModel()
    }

    fun isModified(): Boolean {
        val orgGitlabUrls = project.service<ProjectSettings>().gitlabUrls
        return orgGitlabUrls.size != gitlabUrlTokens.size ||
                gitlabUrlTokens.any {
                    !orgGitlabUrls.contains(it.gitlabAlias) ||
                            AppSettings.instance.getGitlabTokenBlocking(it.gitlabAlias) != it.gitlabToken
                }
    }

    fun apply() {
        gitlabUrlTokens.forEach { AppSettings.instance.saveGitlabToken(it) }
        project.service<ProjectSettings>().gitlabUrls = gitlabUrlTokens.map { it.gitlabAlias }.toSet()
        project.service<ProjectSettings>().gitlabUrlTokens =  gitlabUrlTokens.toList()
    }

    private class GitlabUrlTokenDialog(val project: Project, newGitlabUrlToken: GitlabUrlToken? = null) :
        DialogWrapper(true) {
        var gitlabUrlToken = newGitlabUrlToken ?: GitlabUrlToken()
        val gitlabAliasTextField = JBTextField()
        val gitlabUrlTextField = JBTextField()
        val gitlabTokenTextField = JBTextField()
        val verifyLabel = JLabel()

        init {
            title = message("settings.gitlabUrlToken.dialog")
            setOKButtonText(newGitlabUrlToken?.let { message("actions.update") } ?: message("actions.add"))
            setSize(700, 200)
            init()
        }

        override fun createActions(): Array<Action> {
            super.createActions()
            return arrayOf(createVerifyAction(), okAction, cancelAction)
        }

        override fun createCenterPanel() = panel {
            row(message("settings.gitlab-url-token.gitlab-alias")) {
                cell(gitlabAliasTextField)
                    .align(Align.FILL)
                    .bindText({ gitlabUrlToken.gitlabAlias }, { gitlabUrlToken.gitlabAlias = it })
                    .focused()
                    .validationOnApply { notBlank(it.text) }
                    .comment(message("settings.gitlab-url-token.gitlab-alias.comment"))
                    .emptyText(message("settings.gitlab-url-token.gitlab-alias.empty-text"))
            }
            row(message("settings.gitlab-url-token.gitlab-url")) {
                cell(gitlabUrlTextField)
                    .align(Align.FILL)
                    .bindText({ gitlabUrlToken.gitlabUrl }, { gitlabUrlToken.gitlabUrl = it })
                    .validationOnApply { notBlank(it.text) }
                    .comment(message("settings.gitlab-url-token.gitlab-url.comment"))
                    .emptyText(message("settings.gitlab-url-token.gitlab-url.empty-text"))
            }
            row(message("settings.gitlab-url-token.gitlab-token")) {
                cell(gitlabTokenTextField)
                    .align(Align.FILL)
                    .bindText({ gitlabUrlToken.gitlabToken.orEmpty() }, { gitlabUrlToken.gitlabToken = it })
                    .validationOnApply { notBlank(it.text) }
                    .emptyText(message("settings.gitlab-url-token.gitlab-token.empty-text"))
                    .comment(message("settings.gitlab-url-token.gitlab-token.comment"), action= {
                        val host: String = try {
                            val uri = URL(gitlabUrlTextField.text).toURI()
                            "${uri.scheme}://${uri.host}"
                        } catch (e: Exception) {
                            "https://gitlab.com"
                        }
                        BrowserLauncher.instance.open("$host/-/profile/personal_access_tokens?name=Gitlab+Template+Lint+token&scopes=api,read_api")
                    })
            }
            row {
                cell(verifyLabel)
            }
        }

        private fun createVerifyAction(): DialogWrapperAction {
            return object : DialogWrapperAction(message("settings.verify")) {
                override fun doAction(e: ActionEvent?) {
                    runBackgroundableTask(message("settings.verify.running")) {
                        if (gitlabUrlTextField.text.isEmpty()) {
                            verifyLabel.icon = AllIcons.General.InspectionsError
                            verifyLabel.text = message("settings.verify.gitlab-url-not-set")
                        } else {
                            verifyLabel.icon = AllIcons.General.InlineRefreshHover
                            verifyLabel.text = message("settings.verify.running")
                            try {
                                project.service<Gitlab>()
                                    .getVersion(gitlabUrlTextField.text, gitlabTokenTextField.text.orEmpty())
                                    .get()
                                verifyLabel.icon = AllIcons.General.InspectionsOK
                                verifyLabel.text = message("settings.verify.valid")
                            } catch (e: Exception) {
                                verifyLabel.icon = AllIcons.General.InspectionsError
                                verifyLabel.text =
                                    message("settings.verify.invalid", e.cause?.localizedMessage.orEmpty())
                            }
                        }
                    }
                }
            }
        }
    }
}
