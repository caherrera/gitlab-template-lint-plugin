package com.github.caherrera.gitlab.template.lint.plugin.settings.remote

data class Remote(
    var remoteUrl: String = "",
    var gitlabAlias: String? = null,
    var remoteId: Long? = null
)
