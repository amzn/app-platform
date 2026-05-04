package software.amazon.test

import kotlin.reflect.KClass
import software.amazon.app.platform.robot.Robot

interface TestRobotGraph {
  val robots: Map<KClass<*>, () -> Robot>
}
