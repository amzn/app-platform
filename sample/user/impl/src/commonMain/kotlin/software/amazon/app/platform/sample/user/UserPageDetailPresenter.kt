package software.amazon.app.platform.sample.user

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Input
import software.amazon.app.platform.sample.user.UserPageDetailPresenter.Model

/** Presenter to manage the detail content of the list-detail layout. */
@Inject
class UserPageDetailPresenter(private val sessionTimeout: SessionTimeout) :
  MoleculePresenter<Input, Model> {

  @Composable
  override fun present(input: Input): Model {
    val timeout by sessionTimeout.sessionTimeout.collectAsState()

    return Model(
      text = input.user.attributes[input.selectedAttribute].value,
      pictureKey = input.user.attributes.single { it.key == User.Attribute.PICTURE_KEY }.value,
      timeoutProgress = (timeout / SessionTimeout.initialTimeout).toFloat(),
    )
  }

  /** The state of the detail pane. */
  data class Model(
    /** The text rendered on screen. Usually, refers to the selected user attribute. */
    val text: String,
    /** The profile picture ID loaded from the resources. */
    val pictureKey: String,
    /** The progress until when current user is logged out. The value is between [0, 1]. */
    val timeoutProgress: Float,
  ) : BaseModel

  /**
   * The input type of the presenter. [user] is the currently logged in user. [selectedAttribute] is
   * the index of the selected attribute in the UI.
   */
  data class Input(val user: User, val selectedAttribute: Int)
}
