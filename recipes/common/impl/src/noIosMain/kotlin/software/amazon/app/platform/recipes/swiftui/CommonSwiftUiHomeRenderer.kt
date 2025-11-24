package software.amazon.app.platform.recipes.swiftui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.swiftui.SwiftUiHomePresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer

/**
 * SwiftUI integration isn't supported for platforms other than iOS. There are two methods of
 * rendering this model. One `PresenterView` implemented in Swift and one in the special `noIos`
 * source folder. At runtime depending on the platform the right method is used.
 */
@ContributesRenderer
class CommonSwiftUiHomeRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Box(modifier = Modifier.fillMaxSize()) {
      Text("SwiftUI is only supported on iOS", Modifier.align(Alignment.Center))
    }
  }
}
