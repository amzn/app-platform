package software.amazon.app.platform.sample.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.sample.template.animation.AnimationContentKey
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Input
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Model

/** Presenter to manage the detail content of the list-detail layout. */
@Inject
class UserPageDetailPresenter(private val sessionTimeout: SessionTimeout) :
  MoleculePresenter<Input, Model> {

  @Composable
  override fun present(input: Input): Model {
    val timeout by sessionTimeout.sessionTimeout.collectAsState()

    var showPictureFullscreen by remember { mutableStateOf(false) }

    return Model(
      text = input.user.attributes[input.selectedAttribute].value,
      pictureKey = input.user.attributes.single { it.key == User.Attribute.PICTURE_KEY }.value,
      timeoutProgress = (timeout / SessionTimeout.initialTimeout).toFloat(),
      showPictureFullscreen = showPictureFullscreen,
    ) {
      when (it) {
        Event.ProfilePictureClick -> {
          showPictureFullscreen = !showPictureFullscreen
        }
      }
    }
  }

  /** The state of the detail pane. */
  data class Model(
    /** The text rendered on screen. Usually, refers to the selected user attribute. */
    val text: String,
    /** The profile picture ID loaded from the resources. */
    val pictureKey: String,
    /** The progress until when current user is logged out. The value is between [0, 1]. */
    val timeoutProgress: Float,
    /** If this value is true, then the profile picture is shown in full screen. */
    val showPictureFullscreen: Boolean,
    /** Callback to send events back to the presenter. */
    val onEvent: (Event) -> Unit,
  ) : BaseModel, AnimationContentKey {
    override val contentKey: Int =
      if (showPictureFullscreen) 1 else AnimationContentKey.DEFAULT_CONTENT_KEY
  }

  /** All events that [UserPageDetailPresenter] can process. */
  sealed interface Event {
    /** Sent when the user taps on the profile picture. */
    data object ProfilePictureClick : Event
  }

  /**
   * The input type of the presenter. [user] is the currently logged in user. [selectedAttribute] is
   * the index of the selected attribute in the UI.
   */
  data class Input(val user: User, val selectedAttribute: Int)
}
