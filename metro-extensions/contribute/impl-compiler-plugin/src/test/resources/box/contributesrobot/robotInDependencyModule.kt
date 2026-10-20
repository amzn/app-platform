// MODULE: lib
// FILE: TestRobot.kt
package com.test

import dev.zacsweers.metro.Named
import software.ralf.app.platform.inject.robot.ContributesRobot
import software.ralf.app.platform.robot.Robot

@ContributesRobot(AppScope::class)
class TestRobot(@Named("robot") val value: String) : Robot

// MODULE: main(lib)
// FILE: main.kt
package com.test

import dev.zacsweers.metro.Named
import software.ralf.app.platform.robot.Robot
import software.ralf.app.platform.robot.RobotGraph

@DependencyGraph(AppScope::class)
interface MyGraph {
  @Provides @Named("robot") fun robotValue(): String = "qualified"

  @Provides fun unqualifiedValue(): String = "unqualified"
}

fun bindRobot(
  contribution: TestRobot.RobotContribution,
  robot: TestRobot,
): Robot = contribution.provideTestRobotIntoMap(robot)

fun box(): String {
  assertEquals(
    expected = "direct",
    actual = TestRobot.RobotContribution.provideTestRobot("direct").value,
  )
  val graph = createGraph<MyGraph>() as RobotGraph
  val robot = assertIs<TestRobot>(graph.robots.getValue(TestRobot::class)())
  assertEquals(expected = "qualified", actual = robot.value)
  return "OK"
}
