@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.amazon.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter.Model

/**
 * This presenter manages its own backstack. All presenters in the stack are always active, because
 * Navigation3 renders them during a back gesture. This could be optimized further in the future to
 * compute the model of the top 2 presenters only and remembering the state of all other presenters.
 */
@Inject
class Navigation3HomePresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val backstack = remember {
      mutableStateListOf<MoleculePresenter<Unit, out BaseModel>>().apply {
        // There must be always one element.
        add(Navigation3ChildPresenter(index = 0, backstack = this))
      }
    }

    return Model(backstack = backstack.map { it.present(Unit) }) {
      when (it) {
        Event.Pop -> {
          backstack.removeAt(backstack.size - 1)
        }
      }
    }
  }

  data class Model(val backstack: List<BaseModel>, val onEvent: (Event) -> Unit) :
    BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig = AppBarConfig(title = "Navigation3")
  }

  sealed interface Event {
    data object Pop : Event
  }
}
