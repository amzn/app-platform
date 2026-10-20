// MODULE: lib
// FILE: SuperType.kt
package com.test

interface SuperType

// FILE: TestClass.kt
package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

@Inject
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass : SuperType, Scoped

// MODULE: main(lib)
// FILE: GraphInterface.kt
package com.test

import software.ralf.app.platform.scope.Scoped

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val superTypeInstance: SuperType

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

// FILE: main.kt
package com.test

fun box(): String {
  val graph = createGraph<GraphInterface>()
  assertIs<TestClass>(graph.superTypeInstance)
  assertSame<Any>(expected = graph.superTypeInstance, actual = graph.allScoped.single())
  return "OK"
}
