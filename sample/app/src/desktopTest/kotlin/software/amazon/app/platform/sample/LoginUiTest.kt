package software.amazon.app.platform.sample

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Duration.Companion.seconds
import software.amazon.app.platform.robot.composeRobot
import software.amazon.app.platform.robot.internal.RobotInternals
import software.amazon.app.platform.robot.waitUntilCatching
import software.amazon.app.platform.sample.login.LoginRobot
import software.amazon.app.platform.sample.user.UserPageRobot

@OptIn(ExperimentalTestApi::class)
class LoginUiTest {

  private lateinit var desktopApp: DesktopApp

  @BeforeTest
  fun before() {
    desktopApp = DesktopApp {
      // Note that we use a different test specific component in UI tests.
      TestDesktopAppComponent::class.create(it)
    }

    // This is required for Desktop and iOS. On Android it's expected that the Application
    // class implements the RootScopeProvider interface and the test environment has static
    // access to the Application.
    RobotInternals.setRootScopeProvider(desktopApp)
  }

  @AfterTest
  fun after() {
    RobotInternals.setRootScopeProvider(null)

    // Good hygiene to clean everything up.
    desktopApp.destroy()
  }

  @Test
  fun `a user logs in`(): Unit = runRobotTest {
    composeRobot<LoginRobot> {
      seeLoginButton()
      clickLoginButton()
    }

    waitUntilCatching("login finished", timeout = 2.seconds) {
      composeRobot<UserPageRobot> { seeUserId() }
    }
  }

  /** Convenience function to start rendering templates. */
  private fun runRobotTest(block: ComposeUiTest.() -> Unit) {
    runComposeUiTest {
      setContent { desktopApp.renderTemplates() }

      block()
    }
  }
}
