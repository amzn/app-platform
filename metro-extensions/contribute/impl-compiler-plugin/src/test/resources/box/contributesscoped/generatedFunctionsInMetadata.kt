// WITH_REFLECT

package com.test

import kotlin.reflect.full.declaredFunctions
import software.ralf.app.platform.inject.metro.ContributesScoped
import software.ralf.app.platform.scope.Scoped

interface SuperType

@ContributesScoped(AppScope::class)
class TestClass : SuperType, Scoped

fun box(): String {
  val contribution = TestClass.ScopedContribution::class
  val companion = TestClass.ScopedContribution.Companion::class
  val classes = listOf(contribution, companion)
  val expected = listOf(setOf("bindSuperType", "bindTestClassScoped"), setOf("provideTestClass"))
  assertEquals(
    expected = expected,
    actual = classes.map { type ->
      type.java.declaredMethods.filterNot { it.isSynthetic }.map { it.name }.toSet()
    },
    message = "Generated functions must exist in JVM bytecode",
  )
  assertEquals(
    expected = expected,
    actual = classes.map { type -> type.declaredFunctions.map { it.name }.toSet() },
    message = "Generated functions must exist in Kotlin metadata",
  )
  return "OK"
}
