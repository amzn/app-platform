package software.amazon.app.platform.sample.user

import androidx.compose.runtime.Composable
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.template.ModelDelegate
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.sample.template.SampleAppTemplate
import software.amazon.app.platform.sample.user.UserPagePresenter.Model
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Production implementation of [UserPagePresenter].
 *
 * This class injects two child presenters to compute models. The child presenters have inputs for
 * bi-directional communication between presenters.
 *
 * Note that this class itself doesn't have a [Renderer], because it only combines models from child
 * presenters, which come with a [Renderer].
 */
@Inject
@ContributesBinding(UserScope::class)
class UserPagePresenterImpl(
  private val user: User,
  private val userPageListPresenter: UserPageListPresenter,
  private val userPageDetailPresenter: UserPageDetailPresenter,
) : UserPagePresenter {

  @Composable
  override fun present(input: Unit): Model {
    // Note that listModel provides further input for the detail presenter.
    val listModel = userPageListPresenter.present(UserPageListPresenter.Input(user))
    val detailModel =
      userPageDetailPresenter.present(
        UserPageDetailPresenter.Input(user, selectedAttribute = listModel.selectedIndex)
      )

    return ModelImpl(listModel = listModel, detailModel = detailModel)
  }

  /**
   * This class implements [ModelDelegate] to override which [SampleAppTemplate] to use. This Model
   * hosts to other models [listModel] and [detailModel], which will be produced by child
   * presenters.
   */
  private data class ModelImpl(
    override val listModel: UserPageListPresenter.Model,
    override val detailModel: UserPageDetailPresenter.Model,
  ) : Model, ModelDelegate {
    override fun delegate(): BaseModel {
      return if (detailModel.showPictureFullscreen) {
        SampleAppTemplate.FullScreenTemplate(detailModel)
      } else {
        SampleAppTemplate.ListDetailTemplate(listModel, detailModel)
      }
    }
  }
}
