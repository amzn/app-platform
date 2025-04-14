package software.amazon.app.platform.renderer

import android.app.Activity
import android.view.ViewGroup
import kotlin.reflect.KClass
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider

/**
 * [RendererFactory] that is able to create [ComposeRenderer] and [ViewRenderer] instances for
 * Android, unlike [ComposeRendererFactory] which only handles [ComposeRenderer] and unlike
 * [AndroidRendererFactory] which only handles [ViewRenderer]. Further, this implementation provides
 * adapters for renderers for a seamless interop between Compose UI and Android Views.
 */
public class ComposeAndroidRendererFactory(
  rootScopeProvider: RootScopeProvider,
  activity: Activity,
  parent: ViewGroup,
) : AndroidRendererFactory(rootScopeProvider, activity, parent) {
  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
    val renderer = super.createRenderer(modelType)

    check(renderer !is ComposeWithinAndroidViewRenderer) {
      "Trying to wrap a render that has been wrapped already for model $modelType."
    }

    check(renderer !is AndroidViewWithinComposeRenderer) {
      "Trying to wrap a render that has been wrapped already for model $modelType."
    }

    return when (renderer) {
      // Wrap a ComposeRenderer to support embedding it in an Android View hierarchy.
      is BaseComposeRenderer<*> ->
        @Suppress("UNCHECKED_CAST")
        ComposeWithinAndroidViewRenderer(renderer as BaseComposeRenderer<T>)

      // Wrap a ViewRenderer to support embedding it in a Compose UI hierarchy.
      is BaseAndroidViewRenderer -> AndroidViewWithinComposeRenderer(renderer)

      // Should not happen, each original Renderer is wrapped.
      else -> error("Unsupported renderer type ${renderer::class} for model $modelType.")
    }
  }
}
