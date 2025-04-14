package software.amazon.app.platform.robot

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isTrue
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.addDiComponent

class RobotTest {

  @Test
  fun `if no robot can be found in the component then a proper error is thrown`() {
    val exception = assertFailsWith<IllegalStateException> { robot<TestRobot>(rootScope()) {} }

    val message =
      exception.message?.replace("RobotTest\$TestRobot", "RobotTest.TestRobot").toString()
    assertThat(message)
      .contains(
        "Could not find Robot of type class software.amazon.app.platform." +
          "robot.RobotTest.TestRobot"
      )
    assertThat(message).contains("Did you forget to add the @ContributesRobot annotation?")
  }

  @Test
  fun `the close function is called after the lambda is invoked`() {
    val rootScope = rootScope(TestRobot())

    lateinit var robot: TestRobot
    robot<TestRobot>(rootScope) {
      robot = this
      assertThat(closeCalled).isFalse()
    }

    assertThat(robot.closeCalled).isTrue()
  }

  @Test
  fun `a new robot is instantiated every time the robot function is invoked`() {
    val rootScope =
      Scope.buildRootScope {
        addDiComponent(
          object : RobotComponent {
            override val robots: Map<KClass<out Robot>, () -> Robot> =
              mapOf(TestRobot::class to { TestRobot() })
          }
        )
      }

    lateinit var robot1: TestRobot
    lateinit var robot2: TestRobot

    robot<TestRobot>(rootScope) { robot1 = this }
    robot<TestRobot>(rootScope) { robot2 = this }

    assertThat(robot1).isNotSameInstanceAs(robot2)

    robot<TestRobot>(rootScope) {
      val robot1Inner = this
      robot<TestRobot>(rootScope) {
        val robot2Inner = this
        assertThat(robot1Inner).isNotSameInstanceAs(robot2Inner)
      }
    }
  }

  private fun rootScope(vararg robots: Robot): Scope =
    Scope.buildRootScope { addDiComponent(Component(*robots)) }

  private class Component(vararg robots: Robot) : RobotComponent {
    override val robots: Map<KClass<out Robot>, () -> Robot> =
      robots.associate { robot -> robot::class to { robot } }
  }

  private class TestRobot : Robot {
    var closeCalled = false
      private set

    override fun close() {
      closeCalled = true
    }
  }
}
