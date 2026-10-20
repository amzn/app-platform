package com.test

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import software.ralf.app.platform.scope.Scoped

@SingleIn(AppScope::class)
@DependencyGraph(AppScope::class)
interface Graph {
  @ForScope(AppScope::class) val scoped: Set<Scoped>

  @Provides fun value(): String = "value"

  @Provides fun count(): Int = 7
}
