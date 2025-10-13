package software.amazon.app.platform.recipes

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * The only `Activity` of our recipes app. This class is just an entry point to start rendering
 * templates.
 */
class MainActivity : ComponentActivity() {

  private val rootScopeProvider
    get() = application as RootScopeProvider

  private val viewModel by viewModels<MainActivityViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val intent = Intent(this, FeatureActivity::class.java)
    startActivity(intent)
  }
}
