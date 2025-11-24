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

/** This is a no-op renderer that exists so the RendererFactory doesn't freak out. */
@ContributesRenderer
class IosNoOpSwiftUiHomeRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Box(modifier = Modifier.fillMaxSize()) {
      Text("Rendering the native UI on top", Modifier.align(Alignment.Center))
    }
  }
}
