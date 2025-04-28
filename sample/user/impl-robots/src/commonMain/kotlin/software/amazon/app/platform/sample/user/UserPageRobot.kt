package software.amazon.app.platform.sample.user

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.robot.ContributesRobot
import software.amazon.app.platform.robot.ComposeRobot
import software.amazon.lastmile.kotlin.inject.anvil.AppScope

/**
 * A test robot to verify interactions with the user page screen written with Compose Multiplatform.
 *
 * This robot injects other dependencies from the object graph, e.g. this is helpful to query
 * further data or change behavior of classes. Robots are not exclusive to verifying UI and UI
 * interactions.
 */
@Inject
@ContributesRobot(AppScope::class)
class UserPageRobot(private val userManager: UserManager) : ComposeRobot() {

  private val userIdTextNode
    get() = compose.onNodeWithTag("userIdText")

  private val profilePictureNode
    get() = compose.onNodeWithTag("profilePicture")

  /**
   * Verify that the user ID is displayed. The [userId] can be changed, but uses by default the ID
   * of the logged in user if present.
   */
  fun seeUserId(userId: Long = userManager.user.value?.userId ?: -1L) {
    userIdTextNode.assertIsDisplayed()
    userIdTextNode.assertTextEquals("User: $userId")
  }

  /**
   * Verify that the profile picture is displayed. If [fullScreen] is `true`, then only the picture
   * should be shown and other elements like the user ID [seeUserId] are gone.
   */
  fun seeProfilePicture(fullScreen: Boolean = false) {
    profilePictureNode.assertIsDisplayed()

    if (fullScreen) {
      userIdTextNode.assertDoesNotExist()
    } else {
      userIdTextNode.assertIsDisplayed()
    }
  }

  /** Click on the profile picture. This works in the detail page and fullscreen mode. */
  fun clickProfilePicture() {
    profilePictureNode.performClick()
  }
}
