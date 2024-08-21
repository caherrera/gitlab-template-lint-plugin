package com.github.caherrera.gitlab.template.lint.plugin.pipeline.middleware

import com.github.caherrera.gitlab.template.lint.plugin.git.gitlabUrl
import com.github.caherrera.gitlab.template.lint.plugin.git.httpUrl
import com.github.caherrera.gitlab.template.lint.plugin.git.locateRemote
import com.github.caherrera.gitlab.template.lint.plugin.gitlab.GitlabLintResponse
import com.github.caherrera.gitlab.template.lint.plugin.gitlab.http.toHttps
import com.github.caherrera.gitlab.template.lint.plugin.notifications.Notification
import com.github.caherrera.gitlab.template.lint.plugin.notifications.sendNotification
import com.github.caherrera.gitlab.template.lint.plugin.pipeline.Pass
import com.github.caherrera.gitlab.template.lint.plugin.settings.AppSettings
import com.github.caherrera.gitlab.template.lint.plugin.settings.ProjectSettings
import com.github.caherrera.gitlab.template.lint.plugin.settings.remote.Remote
import com.github.caherrera.gitlab.template.lint.plugin.widget.LintStatusEnum
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import git4idea.repo.GitRepositoryManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Service(Service.Level.PROJECT)
class ResolveContext : Middleware {
    override val priority = 1
    private var showRepositoryNotification = true
    private var showRemoteNotification = true

    override suspend fun invoke(
        pass: Pass,
        next: suspend () -> Pair<GitlabLintResponse?, LintStatusEnum>?
    ): Pair<GitlabLintResponse?, LintStatusEnum>? {

        val repository = locateRepository(pass) ?: return null
        val remote = locateRemote(pass, repository) ?: return null

        pass.repository = repository

        val settings = pass.project.service<ProjectSettings>()
        val remotesMap = AppSettings.instance.remotes
        if (settings.forceHttps) {
            val remoteUrl = remote.httpUrl?.toHttps()?.toString() ?: return null
            pass.remoteUrl = remoteUrl

            val gitlabUrl = remote.gitlabUrl?.toHttps()?.toString()
            pass.gitlabUrl = getGitlabUrlOrCreateRemote(remotesMap, remoteUrl, gitlabUrl!!)
        } else {
            val remoteUrl = remote.httpUrl?.toString() ?: return null
            pass.remoteUrl = remoteUrl

            val gitlabUrl = remote.gitlabUrl?.toString()
            pass.gitlabUrl = getGitlabUrlOrCreateRemote(remotesMap, remoteUrl, gitlabUrl!!)
        }

        return next()
    }

    /*
        * If the remoteUrl is not in the remotesMap, then add it to the map.
        * If the remoteUrl is in the remotesMap, but the gitlabUrl is null, then set the gitlabUrl.
        * If the remoteUrl is in the remotesMap, but the gitlabUrl is not null, then return the gitlabUrl.
     */
    private fun getGitlabUrlOrCreateRemote(
        remotesMap: MutableMap<String, Remote?>,
        remoteUrl: String,
        gitlabUrl: String
    ): String {
        return remotesMap.getOrPut(remoteUrl) {
            Remote(
                remoteUrl,
                gitlabUrl,
                null
            )
        }?.gitlabAlias ?: let {
            remotesMap[remoteUrl] = Remote(remoteUrl, gitlabUrl, null)
            gitlabUrl
        }
    }

    private suspend fun locateRepository(pass: Pass): GitRepository? {
        val repository = withContext(Dispatchers.IO) {
            GitRepositoryManager.getInstance(pass.project).getRepositoryForFile(pass.file.virtualFile)
        }

        showRepositoryNotification = if (repository == null) {
            sendNotification(Notification.repositoryNotFound(), pass.project)
            false
        } else {
            true
        }

        return repository
    }

    private fun locateRemote(pass: Pass, repository: GitRepository): GitRemote? {
        val remote = repository.locateRemote(pass.project.service<ProjectSettings>().remote)

        showRemoteNotification = if (remote == null) {
            sendNotification(Notification.remoteNotFound(), pass.project)
            false
        } else {
            true
        }

        return remote
    }
}
