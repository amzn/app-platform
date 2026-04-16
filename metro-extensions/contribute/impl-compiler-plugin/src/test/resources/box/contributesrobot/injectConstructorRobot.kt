package com.test

import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.Robot
import software.amazon.app.platform.robot.RobotGraph

@Inject
class RobotDependency {
  fun value(): String = "dependency"
}

@Inject
@ContributesRobot(AppScope::class)
class TestRobot(
  val dependency: RobotDependency,
) : Robot

@DependencyGraph(AppScope::class)
interface MyGraph : RobotGraph

fun box(): String {
  val graph = createGraph<MyGraph>()
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  if (robot !is TestRobot) {
    return "FAIL: expected TestRobot but got $robot"
  }

  return if (robot.dependency.value() == "dependency") {
    "OK"
  } else {
    "FAIL: dependency was not injected"
  }
}
