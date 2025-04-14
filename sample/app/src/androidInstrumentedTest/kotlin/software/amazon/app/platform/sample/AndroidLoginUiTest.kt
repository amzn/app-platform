package software.amazon.app.platform.sample

import android.provider.Settings.Global.ANIMATOR_DURATION_SCALE
import android.provider.Settings.Global.TRANSITION_ANIMATION_SCALE
import android.provider.Settings.Global.WINDOW_ANIMATION_SCALE
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.SemanticsNodeInteractionsProvider
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.platform.app.InstrumentationRegistry
import kotlin.time.Duration.Companion.seconds
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import software.amazon.app.platform.robot.ComposeInteractionsProvider
import software.amazon.app.platform.robot.composeRobot
import software.amazon.app.platform.robot.waitUntilCatching
import software.amazon.app.platform.sample.login.LoginRobot
import software.amazon.app.platform.sample.user.UserPageRobot
import software.amazon.app.platform.scope.RootScopeProvider

/** This class implements [ComposeInteractionsProvider] to make it easier to call [composeRobot]. */
class AndroidLoginUiTest : ComposeInteractionsProvider {

  @get:Rule val activityRule = ActivityScenarioRule(MainActivity::class.java)

  @get:Rule
  val composeTestRule: ComposeTestRule =
    AndroidComposeTestRule(activityRule, ::getActivityFromTestRule)

  override val semanticsNodeInteractionsProvider: SemanticsNodeInteractionsProvider
    get() = composeTestRule

  @Before
  fun before() {
    setAnimations(enabled = false)
  }

  @After
  fun after() {
    val rootScopeProvider =
      InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
        as RootScopeProvider

    // Good hygiene to clean everything up.
    rootScopeProvider.rootScope.destroy()
    setAnimations(enabled = true)
  }

  @Test
  fun a_user_logs_in() {
    composeRobot<LoginRobot> {
      seeLoginButton()
      clickLoginButton()
    }

    waitUntilCatching("login finished", timeout = 4.seconds) {
      composeRobot<UserPageRobot> { seeUserId() }
    }
  }

  // Borrowed from AndroidComposeTestRule.
  private fun <A : ComponentActivity> getActivityFromTestRule(rule: ActivityScenarioRule<A>): A {
    var activity: A? = null
    rule.scenario.onActivity { activity = it }

    return with(activity) {
      checkNotNull(this) { "Activity was not set in the ActivityScenarioRule!" }
    }
  }

  private fun setAnimations(enabled: Boolean) {
    val value = if (enabled) "1.0" else "0.0"
    InstrumentationRegistry.getInstrumentation().uiAutomation.run {
      executeShellCommand("settings put global $WINDOW_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $TRANSITION_ANIMATION_SCALE $value")
      executeShellCommand("settings put global $ANIMATOR_DURATION_SCALE $value")
    }
  }
}
