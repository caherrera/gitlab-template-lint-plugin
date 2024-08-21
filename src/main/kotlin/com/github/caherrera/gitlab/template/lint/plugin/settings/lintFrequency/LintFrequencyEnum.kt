package com.github.caherrera.gitlab.template.lint.plugin.settings.lintFrequency

import com.github.caherrera.gitlab.template.lint.plugin.GitlabLintBundle.message

enum class LintFrequencyEnum(val message: String) {
    ON_CHANGE(message("settings.frequency.on-change")),
    ON_SAVE(message("settings.frequency.on-save")),
    MANUAL(message("settings.frequency.manual"))
}