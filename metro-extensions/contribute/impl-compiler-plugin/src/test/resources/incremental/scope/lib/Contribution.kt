package com.test

import dev.zacsweers.metro.SingleIn
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

object FirstScope

object SecondScope

@ContributesScoped(FirstScope::class) @SingleIn(FirstScope::class) class Contribution : Scoped
