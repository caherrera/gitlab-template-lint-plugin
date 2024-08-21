package com.github.caherrera.gitlab.template.lint.plugin.git

import git4idea.GitUtil
import git4idea.repo.GitRepository

fun GitRepository.locateRemote(name: String) = GitUtil.findRemoteByName(this, name)

