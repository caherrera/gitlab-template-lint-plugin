package com.github.caherrera.gitlab.template.lint.plugin.settings

import com.github.caherrera.gitlab.template.lint.plugin.settings.gitlabUrlToken.GitlabUrlToken
import com.github.caherrera.gitlab.template.lint.plugin.settings.gitlabUrlToken.GitlabUrlTokenTable
import com.github.caherrera.gitlab.template.lint.plugin.settings.lintFrequency.LintFrequencyEnum
import com.github.caherrera.gitlab.template.lint.plugin.settings.remote.Remote
import com.intellij.credentialStore.CredentialAttributes
import com.intellij.credentialStore.Credentials
import com.intellij.ide.passwordSafe.PasswordSafe
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.diagnostic.Logger
import com.intellij.util.xmlb.XmlSerializerUtil
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import kotlin.math.log


/**
 * Supports storing the application settings in a persistent way.
 * The [State] and [Storage] annotations define the name of the data and the file name where
 * these persistent application settings are stored.
 */
@State(
    name = AppSettings.SERVICE_NAME,
    storages = [Storage("GitlabLint.xml")]
)
class AppSettings : PersistentStateComponent<AppSettings> {
    companion object {
        const val SERVICE_NAME = "com.github.caherrera.gitlab.template.lint.plugin.settings.AppSettingsState"
        val instance: AppSettings
            get() = ApplicationManager.getApplication().getService(AppSettings::class.java)
    }

    // Inclusion globs - do not rename to avoid breaking existing settings
    var gitlabLintGlobStrings: List<String> = listOf("**/*.gitlab-ci.yml", "**/*.gitlab-ci.yaml")
    var exclusionGlobs: List<String> = listOf()
    var lastVersion: String? = null
    var hits = 0
    var requestSupport = true

    // Remote url to Remote
    var remotes: MutableMap<String, Remote?> = mutableMapOf()
    var lintFrequency: LintFrequencyEnum = LintFrequencyEnum.ON_SAVE
    var showMergedPreview = true
    var allowSelfSignedCertificate = false
    var runLintOnFileChange = true
    private var logger = Logger.getInstance(AppSettings::class.java)


    fun saveGitlabToken(gitlabToken: GitlabUrlToken) {
        logger.info("Saving token for ${gitlabToken}")
        val credentialAttributes = getCredentialAttributes(gitlabToken.gitlabAlias)
        logger.info(" >>> ${credentialAttributes}")
        PasswordSafe.instance.setPassword(credentialAttributes, gitlabToken.gitlabToken)
    }

    suspend fun getGitlabToken(gitlabAlias: String): String? {
        logger.info("Getting token for ${gitlabAlias}")
        val credentialAttributes = getCredentialAttributes(gitlabAlias)
        logger.info(" >>> ${credentialAttributes}")

        val credentials: Credentials? = withContext(Dispatchers.IO) {
            return@withContext PasswordSafe.instance.get(credentialAttributes)
        }
        return credentials?.getPasswordAsString()
    }

    fun getGitlabTokenBlocking(gitlabAlias: String): String? {
        return runBlocking {
            return@runBlocking getGitlabToken(gitlabAlias)
        }
    }

    private fun getCredentialAttributes(title: String): CredentialAttributes {
        logger.info("Getting credential attributes for ${title}")
        val c = CredentialAttributes(
            title,
            null,
            this.javaClass,
            false
        )
        logger.info("Credential :: ${c}")
        return c;
    }

    @Nullable
    override fun getState() = this


    override fun loadState(@NotNull state: AppSettings) {
        XmlSerializerUtil.copyBean(state, this)
    }

    fun recordHit() {
        hits++
    }
}
