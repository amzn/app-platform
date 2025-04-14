package software.amazon.app.platform.renderer

import androidx.compose.runtime.Composable
import software.amazon.app.platform.presenter.BaseModel

/**
 * An implementation of [Renderer] that is specific to Compose UI Multiplatform.
 *
 * Your custom renderers should extend this abstract class and provide implementations for
 * [Compose]. This function gets called each time a new [BaseModel] is available and the UI elements
 * should be updated, e.g.
 *
 * ```
 * @ContributesRenderer
 * class MyRenderer : ComposeRenderer<MyModel>() {
 *     @Composable
 *     override fun Compose(model: MyModel) {
 *         Text(
 *             text = "MyModel value: ${model.value}",
 *         )
 *     }
 * }
 * ```
 *
 * It's strongly recommended to follow Compose UI best practices, e.g. state should be retained
 * within the [Compose] function between updates and not as a field of the [ComposeRenderer], e.g.
 *
 * ```
 * // Do this
 * @Composable
 * override fun Compose(model: MyModel) {
 *     var string by remember { mutableStateOf(..) }
 * }
 *
 * // DO NOT DO THIS
 * private var string: String? = null
 *
 * @Composable
 * override fun Compose(model: MyModel) {
 *     string = ...
 * }
 * ```
 *
 * While [ComposeRenderer] implements the [Renderer] interface for seamless integration in the whole
 * stack, the [Renderer.render] function is not supported and throws an error. The function itself
 * is deprecated and hidden. Instead, [renderCompose] should be called, which preserves the Compose
 * UI context.
 */
public abstract class ComposeRenderer<in ModelT : BaseModel> :
  BaseComposeRenderer<ModelT>, Renderer<ModelT> {

  @Deprecated(
    message = "ComposeRenderers must invoke renderCompose(model)",
    level = DeprecationLevel.HIDDEN,
  )
  final override fun render(model: ModelT): Nothing {
    error("ComposeRenderers must invoke renderCompose(model).")
  }

  @Composable
  final override fun renderCompose(model: ModelT) {
    // This function seems redundant and implementations could implement it instead of the
    // separate Compose() function. However, it will allow us to intercept rendering calls
    // in the platform for future use cases. Compare this with ViewRenderer.render() and
    // ViewRenderer.renderModel().
    Compose(model)
  }

  /** Render the given [model] on screen using Compose UI. */
  @Suppress("FunctionName") @Composable protected abstract fun Compose(model: ModelT)
}
