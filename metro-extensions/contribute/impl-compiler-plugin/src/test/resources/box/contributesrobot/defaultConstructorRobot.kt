package com.test

import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.Robot
import software.amazon.app.platform.robot.RobotGraph

@ContributesRobot(AppScope::class)
class TestRobot : Robot

@DependencyGraph(AppScope::class)
interface MyGraph : RobotGraph

fun box(): String {
  val graph = createGraph<MyGraph>()
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  return if (robot is TestRobot) "OK" else "FAIL: expected TestRobot but got $robot"
}
