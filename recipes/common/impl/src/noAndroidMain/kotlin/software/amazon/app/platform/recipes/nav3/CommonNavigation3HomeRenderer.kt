package software.amazon.app.platform.recipes.nav3

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.nav3.Navigation3HomePresenter.Model
import software.amazon.app.platform.renderer.ComposeRenderer

/**
 * Navigation3 isn't supported for platforms other than Android yet. There are two renderers for
 * this model. One only in the Android source folder and one in the special "noAndroid" source
 * folder. At runtime depending on the platform the right renderer is used.
 */
@ContributesRenderer
class CommonNavigation3HomeRenderer : ComposeRenderer<Model>() {
  @Composable
  override fun Compose(model: Model) {
    Box(modifier = Modifier.fillMaxSize()) {
      Text("Navigation3 is only supported on Android", Modifier.align(Alignment.Center))
    }
  }
}
