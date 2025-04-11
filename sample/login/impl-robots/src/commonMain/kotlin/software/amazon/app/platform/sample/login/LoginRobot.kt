package software.amazon.app.platform.sample.login

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.ComposeRobot
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

/** A test robot to verify interactions with the login screen written with Compose Multiplatform. */
@ContributesRobot(AppScope::class)
class LoginRobot : ComposeRobot() {

  private val loginButtonNode
    get() = compose.onNodeWithTag("loginButton")

  /** Verify that login button is displayed. */
  fun seeLoginButton() {
    loginButtonNode.assertIsDisplayed()
  }

  /** Clicks the login button and starts the login process. */
  fun clickLoginButton() {
    loginButtonNode.performClick()
  }
}
