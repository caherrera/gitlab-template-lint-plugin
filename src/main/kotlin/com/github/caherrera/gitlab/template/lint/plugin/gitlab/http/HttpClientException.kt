package com.github.caherrera.gitlab.template.lint.plugin.gitlab.http

class HttpClientException : RuntimeException {
    constructor(message: String?, cause: Throwable?) : super(message, cause)
}
