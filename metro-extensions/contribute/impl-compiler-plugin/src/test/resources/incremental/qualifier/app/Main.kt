package com.test

import dev.zacsweers.metro.createGraph
import software.ralf.app.platform.robot.RobotGraph

fun main() {
  val graph = createGraph<Graph>() as RobotGraph
  val robot = graph.robots.getValue(TestRobot::class)() as TestRobot
  println("RESULT:${robot.value}")
}
