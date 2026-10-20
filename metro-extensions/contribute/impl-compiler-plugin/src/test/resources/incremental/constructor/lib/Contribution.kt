package com.test

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestScoped(val value: String) : Scoped
