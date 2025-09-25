package software.amazon.app.platform.recipes.backstack

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.common.impl.databinding.CrossSlideViewBinding
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.ViewBindingRenderer
import software.amazon.app.platform.renderer.getRenderer

@Inject
@ContributesRenderer
class AndroidCrossSlideBackstackRenderer(
  private val rendererFactory: RendererFactory,
) : ViewBindingRenderer<CrossSlideBackstackPresenter.Model, CrossSlideViewBinding>() {
  override fun inflateViewBinding(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: CrossSlideBackstackPresenter.Model
  ): CrossSlideViewBinding = CrossSlideViewBinding.inflate(layoutInflater, parent, false)

  private var lastRenderer: Renderer<*>? = null

  override fun renderModel(model: CrossSlideBackstackPresenter.Model) {
    val modelToRender = model.delegate
    val renderer = rendererFactory.getRenderer(
      modelToRender::class,
      binding.childContainer,
      modelToRender.hashCode(),
    )

    // commenting in the below code ensures the feature does not crash
//    if (renderer !== lastRenderer) {
//      binding.childContainer.removeAllViews()
//      lastRenderer = renderer
//    }

    renderer.render(modelToRender)
  }
}
