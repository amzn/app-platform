package software.amazon.app.platform.recipes.backstack

import androidx.compose.runtime.Composable
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter.Model

/**
 * A generic presenter that wraps a presenter backstack to play a cross-fade animation whenever a
 * presenter is pushed to the stack or popped from the stack. A backstack always contains
 * [initialPresenter] as an element.
 */
class CrossSlideBackstackPresenter(
  private val initialPresenter: MoleculePresenter<Unit, out BaseModel>
) : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    return presenterBackstack(initialPresenter) { model ->
      Model(delegate = model, backstackScope = this)
    }
  }

  /**
   * The model containing all information about the backstack to play a cross-slide animation in the
   * renderer. [delegate] refers to the model of the top most presenter in the stack.
   * [backstackScope] contains the backstack and allows you to modify the stack.
   */
  data class Model(val delegate: BaseModel, val backstackScope: PresenterBackstackScope) :
    BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      return if (delegate is AppBarConfigModel) {
        delegate.appBarConfig()
      } else {
        AppBarConfig.DEFAULT
      }
    }
  }
}
