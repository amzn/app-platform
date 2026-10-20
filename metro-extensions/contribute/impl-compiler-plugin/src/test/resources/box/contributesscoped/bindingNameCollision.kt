package com.test

import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface TestClassScoped

@Inject
@SingleIn(AppScope::class)
@ContributesScoped(AppScope::class)
class TestClass : TestClassScoped, Scoped

@DependencyGraph(AppScope::class)
@SingleIn(AppScope::class)
interface GraphInterface {
  val instance: TestClassScoped

  @ForScope(AppScope::class)
  val allScoped: Set<Scoped>
}

fun box(): String {
  val graph = createGraph<GraphInterface>()
  assertIs<TestClass>(graph.instance)
  assertSame<Any>(expected = graph.instance, actual = graph.allScoped.single())
  return "OK"
}
