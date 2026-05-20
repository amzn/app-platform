@file:OptIn(ExperimentalAppPlatform::class)
@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.amazon.app.platform.recipes.backstack

import androidx.compose.runtime.Composable
import software.amazon.app.platform.ExperimentalAppPlatform
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.backstack.nav3.PresenterBackstackModel
import software.amazon.app.platform.presenter.backstack.nav3.presenterBackstack
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter.Model

/**
 * A presenter that wraps a presenter backstack to play a cross-slide animation whenever a presenter
 * is pushed to the stack or popped from the stack. A backstack always contains [initialPresenter]
 * as an element.
 */
class CrossSlideBackstackPresenter(
  private val initialPresenter: MoleculePresenter<Unit, out BaseModel>
) : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    return presenterBackstack(initialPresenter) { backstack ->
      Model(backstack = backstack, onBack = { pop() })
    }
  }

  data class Model(override val backstack: List<BaseModel>, override val onBack: () -> Unit) :
    PresenterBackstackModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      val activeModel = backstack.lastOrNull()
      val appBarConfig =
        if (activeModel is AppBarConfigModel) {
          activeModel.appBarConfig()
        } else {
          AppBarConfig.DEFAULT
        }

      return appBarConfig.copy(backArrowAction = onBack.takeIf { backstack.size > 1 })
    }
  }
}
