package com.github.caherrera.gitlab.template.lint.plugin.gitlab.http

import java.net.URI
import java.net.URL

@Suppress("unused")
val URI.domain: URI get() { return URI(host) }

fun URI.toHttps(): URI {
    val url = toURL()

    if (url.protocol == "https") {
        return this
    }

    var normalised = url.file

    url.ref?.let {
        normalised = normalised.plus("#".plus(it))
    }

    return URL("https", host, port, normalised).toURI()
}

@Suppress("unused")
fun URI.withTrimmedPath(): URI {
    val url = toURL()

    if (!url.path.endsWith("/")) {
        return this
    }

    var normalisedFile = url.path.trimEnd('/')

    url.query?.let {
        normalisedFile = normalisedFile.plus("?${it}")
    }

    url.ref?.let {
        normalisedFile = normalisedFile.plus("#${it}")
    }

    return URL(url.protocol, host, port, normalisedFile).toURI()
}
