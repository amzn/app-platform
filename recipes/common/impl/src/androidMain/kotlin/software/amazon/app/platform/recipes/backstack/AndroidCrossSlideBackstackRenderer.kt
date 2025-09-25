package software.amazon.app.platform.recipes.backstack

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.view.animation.Animation
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.common.impl.databinding.CrossSlideViewBinding
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.ViewBindingRenderer
import software.amazon.app.platform.renderer.getRenderer
import software.amazon.app.platform.recipes.common.impl.R

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
  private var lastAnimationContentKey: Int? = null

  override fun renderModel(model: CrossSlideBackstackPresenter.Model) {
    val modelToRender = model.delegate
    val renderer = rendererFactory.getRenderer(
      modelToRender::class,
      binding.childContainer,
      model.hashCode(),
    )

    val animationContentKey = model::class.hashCode() + model.backstackScope.lastBackstackChange.value.backstack.size
    val isPop = model.backstackScope.lastBackstackChange.value.action == PresenterBackstackScope.BackstackChange.Action.POP

    if (animationContentKey != lastAnimationContentKey) {
      val oldView = binding.childContainer.getChildAt(binding.childContainer.childCount - 1)

      val slideOutAnim = if (isPop) {
        AnimationUtils.loadAnimation(activity, R.anim.slide_out_right)
      } else {
        AnimationUtils.loadAnimation(activity, R.anim.slide_out_left)
      }

      val slideInAnim = if (isPop) {
        AnimationUtils.loadAnimation(activity, R.anim.slide_in_left)
      } else {
        AnimationUtils.loadAnimation(activity, R.anim.slide_in_right)
      }

      renderer.render(modelToRender)
      val newView = binding.childContainer.getChildAt(binding.childContainer.childCount - 1)

      oldView?.startAnimation(slideOutAnim)
      newView?.startAnimation(slideInAnim)
    } else {
      renderer.render(modelToRender)
    }

    lastAnimationContentKey = animationContentKey
    lastRenderer = renderer

  }
}
