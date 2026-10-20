package com.test

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.Provides

@DependencyGraph(AppScope::class)
interface Graph {
  @Provides @Named("before") fun before(): String = "before"

  @Provides @Named("after") fun after(): String = "after"
}
