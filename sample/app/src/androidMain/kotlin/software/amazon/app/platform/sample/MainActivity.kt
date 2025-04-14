package software.amazon.app.platform.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.ComposeView
import software.amazon.app.platform.renderer.ComposeAndroidRendererFactory
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
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
//    setContentView(R.layout.activity_main)

    val composeView = ComposeView(this)
    setContentView(composeView)

    composeView.setContent {
      val factory = remember {
        ComposeRendererFactory(
          rootScopeProvider = rootScopeProvider,
//          activity = this,
//          parent = findViewById(R.id.main_container),
        )
      }

      val presenter = remember {
        viewModel.factory.createSampleAppTemplatePresenter(viewModel.navigationPresenter)
      }

      val template = presenter.present(Unit)
      factory.getComposeRenderer(template).renderCompose(template)
    }

//    setCont

//    lifecycleScope.launch {
//      repeatOnLifecycle(Lifecycle.State.STARTED) {
//
//
////        viewModel.templates.collect { template ->
////          val renderer = rendererFactory.getRenderer(template)
////          renderer.render(template)
////        }
//      }
//    }
  }
}
