package software.amazon.app.platform.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.app.platform.renderer.getRenderer
import software.amazon.app.platform.sample.app.R
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * The only `Activity` of our sample app. This class is just an entry point to start rendering
 * templates.
 */
class MainActivity : ComponentActivity() {

  private val rootScopeProvider
    get() = application as RootScopeProvider

  private val viewModel by viewModels<MainActivityViewModel>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()

    setContentView(R.layout.activity_main)

    val rendererFactory =
      ComposeAndroidRendererFactory(
        rootScopeProvider = rootScopeProvider,
        activity = this,
        parent = findViewById(R.id.main_container),
      )

    lifecycleScope.launch {
      repeatOnLifecycle(Lifecycle.State.STARTED) {
        viewModel.templates.collect { template ->
          val renderer = rendererFactory.getRenderer(template)
          renderer.render(template)
        }
      }
    }
  }
}
