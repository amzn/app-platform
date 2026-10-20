package com.test

import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Multibinds
import dev.zacsweers.metro.SingleIn
import software.ralf.app.platform.scope.Scoped

@DependencyGraph(FirstScope::class)
@SingleIn(FirstScope::class)
interface FirstGraph {
  @ForScope(FirstScope::class) @Multibinds(allowEmpty = true) val scoped: Set<Scoped>
}

@DependencyGraph(SecondScope::class)
@SingleIn(SecondScope::class)
interface SecondGraph {
  @ForScope(SecondScope::class) @Multibinds(allowEmpty = true) val scoped: Set<Scoped>
}
