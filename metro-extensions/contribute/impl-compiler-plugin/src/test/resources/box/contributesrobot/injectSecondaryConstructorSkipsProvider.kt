package com.test

import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.Robot
import software.amazon.app.platform.robot.RobotGraph

@Inject
class RobotDependency {
  fun value(): String = "dependency"
}

@ContributesRobot(AppScope::class)
class TestRobot private constructor(
  val dependency: RobotDependency,
  val marker: String,
) : Robot {
  @Inject constructor(dependency: RobotDependency) : this(dependency, "injected")
}

@DependencyGraph(AppScope::class)
interface MyGraph : RobotGraph

fun box(): String {
  val provider =
    TestRobot.RobotContribution::class.java.declaredMethods.singleOrNull {
      it.name == "provideTestRobot"
    }
  if (provider != null) {
    return "FAIL: expected generated provider to be skipped"
  }

  val graph = createGraph<MyGraph>()
  val robotFactory = graph.robots.getValue(TestRobot::class)
  val robot = robotFactory()

  if (robot !is TestRobot) {
    return "FAIL: expected TestRobot but got $robot"
  }

  return if (robot.dependency.value() == "dependency" && robot.marker == "injected") {
    "OK"
  } else {
    "FAIL: dependency was not injected through secondary constructor"
  }
}
