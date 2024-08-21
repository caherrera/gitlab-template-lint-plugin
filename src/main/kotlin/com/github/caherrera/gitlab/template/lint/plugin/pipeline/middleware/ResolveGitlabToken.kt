package com.github.caherrera.gitlab.template.lint.plugin.pipeline.middleware

import com.github.caherrera.gitlab.template.lint.plugin.gitlab.GitlabLintResponse
import com.github.caherrera.gitlab.template.lint.plugin.notifications.Notification
import com.github.caherrera.gitlab.template.lint.plugin.notifications.sendNotification
import com.github.caherrera.gitlab.template.lint.plugin.pipeline.Pass
import com.github.caherrera.gitlab.template.lint.plugin.settings.AppSettings
import com.github.caherrera.gitlab.template.lint.plugin.widget.LintStatusEnum
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger

@Service(Service.Level.PROJECT)
class ResolveGitlabToken : Middleware {
    override val priority = 5
    private var showGitlabTokenNotification = true
    private var logger = Logger.getInstance(ResolveGitlabToken::class.java)

    override suspend fun invoke(
        pass: Pass,
        next: suspend () -> Pair<GitlabLintResponse?, LintStatusEnum>?
    ): Pair<GitlabLintResponse?, LintStatusEnum>? {

        val gitlabToken = resolveGitlabToken(pass) ?: return null

        pass.gitlabToken = gitlabToken

        return next()
    }

    private suspend fun resolveGitlabToken(pass: Pass) : String? {
        logger.info("Resolving Gitlab Token ${pass}")
        val gitlabAlias = pass.gitlabAliasOrThrow()

        val gitlabToken = AppSettings.instance.getGitlabToken(gitlabAlias)
        if (gitlabToken.isNullOrEmpty() && showGitlabTokenNotification) {
            sendNotification(Notification.gitlabTokenNotSet(pass.project), pass.project)
            showGitlabTokenNotification = false
        }
        else if (!gitlabToken.isNullOrEmpty()) {
            showGitlabTokenNotification = true
        }

        return if (gitlabToken.isNullOrEmpty()) return null else gitlabToken
    }

}
