package com.github.caherrera.gitlab.template.lint.plugin

import com.intellij.DynamicBundle
import com.intellij.ide.browsers.BrowserLauncher
import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import com.intellij.openapi.options.ShowSettingsUtil
import com.intellij.openapi.project.Project
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.net.URL

@NonNls
private const val BUNDLE = "messages.MyBundle"

object GitlabLintBundle : DynamicBundle(BUNDLE) {
    val URL_BUG_REPORT = URL("https://github.com/caherrera/gitlab-template-lint-plugin/issues")

    @Suppress("SpreadOperator")
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    @Suppress("SpreadOperator", "unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)

    fun openPluginSettings(project: Project) {
        ShowSettingsUtil.getInstance().showSettingsDialog(project, message("settings.general.group.title"))
    }

    fun openRepository() {
        BrowserLauncher.instance.open("https://github.com/caherrera/gitlab-template-lint-plugin")
    }

    fun openPipelineFilteredIssue() {
        BrowserLauncher.instance.open("https://github.com/caherrera/gitlab-template-lint-plugin/issues/118#issuecomment-1538732251")
    }

    fun plugin() = PluginManagerCore.getPlugin(PluginId.getId("com.github.caherrera.gitlab-template-lint-plugin"))

}
