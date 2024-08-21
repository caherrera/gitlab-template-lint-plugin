package com.github.caherrera.gitlab.template.lint.plugin.settings.gitlabUrlToken

import kotlinx.serialization.Serializable

@Suppress("PROVIDED_RUNTIME_TOO_LOW")
@Serializable
data class GitlabUrlToken(
  var gitlabAlias: String = "",
  var gitlabUrl: String = "https://gitlab.com",
  var gitlabToken: String? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other?.javaClass != javaClass) return false

        other as GitlabUrlToken
        return gitlabAlias == other.gitlabAlias
    }

    override fun hashCode(): Int {
        return gitlabUrl.hashCode()
    }
}
