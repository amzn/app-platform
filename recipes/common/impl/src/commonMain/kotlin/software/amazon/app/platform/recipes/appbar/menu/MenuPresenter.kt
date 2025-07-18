@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.amazon.app.platform.recipes.appbar.menu

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.recipes.appbar.AppBarConfigModel
import software.amazon.app.platform.recipes.appbar.menu.MenuPresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer

/** This presenter provides a custom menu in the App Bar. */
class MenuPresenter : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    var itemCount by remember { mutableIntStateOf(2) }

    val items =
      List(itemCount) {
        val number = it + 1
        AppBarConfig.MenuItem(
          text = "Option $number",
          action = { println("Option $number was pressed.") },
        )
      }

    return Model(items) {
      when (it) {
        Event.AddMenuItem -> itemCount += 1
      }
    }
  }

  data class Model(
    private val menuItems: List<AppBarConfig.MenuItem>,
    val onEvent: (Event) -> Unit,
  ) : BaseModel, AppBarConfigModel {
    override fun appBarConfig(): AppBarConfig {
      return AppBarConfig(title = "Menu items", menuItems = menuItems)
    }
  }

  sealed interface Event {
    data object AddMenuItem : Event
  }
}

@ContributesRenderer
class MenuRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Column(
      modifier = Modifier.fillMaxSize().padding(top = 12.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Button(onClick = { model.onEvent(MenuPresenter.Event.AddMenuItem) }) { Text("Add menu item") }
    }
  }
}
