package com.github.caherrera.gitlab.template.lint.plugin.extensions

fun <T> List<T>.replaceAt(index: Int, value: T): List<T> {
    val mutable = this.toMutableList()

    mutable[index] = value

    return mutable.toList()
}