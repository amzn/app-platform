// WITH_REFLECT

package com.test

import kotlin.reflect.full.declaredFunctions
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

@ContributesRobot(AppScope::class)
class TestRobot : Robot

fun box(): String {
  val contribution = TestRobot.RobotContribution::class
  val companion = TestRobot.RobotContribution.Companion::class
  val classes = listOf(contribution, companion)
  val expected = listOf(setOf("provideTestRobotIntoMap"), setOf("provideTestRobot"))
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
