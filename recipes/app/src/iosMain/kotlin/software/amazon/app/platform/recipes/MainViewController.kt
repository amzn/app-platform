package software.amazon.app.platform.recipes

import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import co.touchlab.kermit.Logger
import platform.UIKit.UIViewController
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.recipes.backstack.CrossSlideBackstackPresenter
import software.amazon.app.platform.recipes.swiftui.SwiftUiHomePresenter
import software.amazon.app.platform.recipes.template.RecipesAppTemplate
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.kotlinInjectComponent

/**
 * This function is called from Swift to hook up the Compose Multiplatform UI.
 *
 * This is our entry point to start producing templates and hooking up our [Renderer] runtime. Other
 * platforms extract this code into classes that are effectively singletons. But this approach is
 * good enough for the iOS recipes app.
 */
@Suppress("unused")
fun mainViewController(rootScopeProvider: RootScopeProvider, renderSwiftUi: (BaseModel) -> Unit): UIViewController =
  ComposeUIViewController {
    // Create a single instance.
    val templateProvider = remember {
      rootScopeProvider.rootScope
        .kotlinInjectComponent<IosAppComponent>()
        .templateProviderFactory
        .createTemplateProvider()
    }

    DisposableEffect(Unit) {
      onDispose {
        // Cancel the provider when it's no longer needed.
        templateProvider.cancel()
      }
    }

    // Only a single factory is needed.
    val factory = remember { ComposeRendererFactory(rootScopeProvider) }

    // Render templates using our Renderer runtime.
    val template by templateProvider.templates.collectAsState()

    // TODO: do something about this.....
    val swiftUiModel =
      ((template as? RecipesAppTemplate.FullScreenTemplate)?.model
        as? CrossSlideBackstackPresenter.Model)?.delegate as? SwiftUiHomePresenter.Model

    if (swiftUiModel == null) {
      val renderer = factory.getRenderer(template::class)

      renderer.renderCompose(template)
    } else {
      renderSwiftUi(swiftUiModel)
    }
  }
