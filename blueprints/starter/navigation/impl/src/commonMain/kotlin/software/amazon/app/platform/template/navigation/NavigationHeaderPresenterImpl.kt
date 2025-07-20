package software.amazon.app.platform.template.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.template.navigation.NavigationHeaderPresenter.Model
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

@Inject
@ContributesBinding(AppScope::class)
class NavigationHeaderPresenterImpl() : NavigationHeaderPresenter {
  @Composable
  override fun present(input: Unit): Model {
    var clickedCount by remember { mutableStateOf(0) }

    return Model(clickedCount = clickedCount) {
      when (it) {
        NavigationHeaderPresenter.Event.Clicked -> {
          clickedCount++
        }
      }
    }
  }
}
