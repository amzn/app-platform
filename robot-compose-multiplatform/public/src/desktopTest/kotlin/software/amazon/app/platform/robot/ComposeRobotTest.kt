package software.amazon.app.platform.robot

import androidx.compose.material.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.reflect.KClass
import kotlin.test.Test
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.addDiComponent

// Note that this class has to be duplicated and cannot be moved into commonTest, because Android
// unit tests don't have access to `runComposeUiTest`.
@OptIn(ExperimentalTestApi::class)
class ComposeRobotTest {

  @Test
  fun `the close function is called after the lambda is invoked`() {
    val rootScope = rootScope(TestRobot())

    lateinit var robot: TestRobot

    runComposeUiTest {
      with(interactionProvider()) {
        composeRobot<TestRobot>(rootScope) {
          robot = this
          assertThat(closeCalled).isFalse()
        }
      }
    }
    assertThat(robot.closeCalled).isTrue()
  }

  @Test
  fun `the SemanticsNodeInteractionsProvider is applied within the composeRobot function`() {
    val rootScope = rootScope(TestRobot())

    runComposeUiTest {
      setContent { Text("Hello world!", Modifier.testTag("text")) }

      with(interactionProvider()) { composeRobot<TestRobot>(rootScope) { textIsShown() } }
    }
  }

  private fun rootScope(vararg robots: Robot): Scope = Scope.buildRootScope { addDiComponent(Component(*robots)) }

  private fun ComposeUiTest.interactionProvider(): ComposeInteractionsProvider {
    val interactionsProvider = this
    return object : ComposeInteractionsProvider {
      override val semanticsNodeInteractionsProvider: SemanticsNodeInteractionsProvider = interactionsProvider
    }
  }

  private class Component(vararg robots: Robot) : RobotComponent {
    override val robots: Map<KClass<out Robot>, () -> Robot> = robots.map { robot -> robot::class to { robot } }.toMap()
  }

  private class TestRobot : ComposeRobot() {
    var closeCalled = false
      private set

    fun textIsShown() {
      compose.onNodeWithTag("text").assertTextEquals("Hello world!")
    }

    override fun close() {
      closeCalled = true
    }
  }
}
