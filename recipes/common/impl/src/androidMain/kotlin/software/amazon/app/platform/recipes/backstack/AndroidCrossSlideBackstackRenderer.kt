package software.amazon.app.platform.recipes.backstack

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.os.Build
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.common.impl.R
import software.amazon.app.platform.recipes.common.impl.databinding.CrossSlideViewBinding
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.ViewBindingRenderer
import software.amazon.app.platform.renderer.getRenderer

@Inject
@ContributesRenderer
class AndroidCrossSlideBackstackRenderer(private val rendererFactory: RendererFactory) :
  ViewBindingRenderer<CrossSlideBackstackPresenter.Model, CrossSlideViewBinding>() {
  override fun inflateViewBinding(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: CrossSlideBackstackPresenter.Model,
  ): CrossSlideViewBinding = CrossSlideViewBinding.inflate(layoutInflater, parent, false)
  private var lastAnimationContentKey: Int? = null

  override fun renderModel(model: CrossSlideBackstackPresenter.Model) {
    binding.lightModeButton.setOnClickListener {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

      activity.recreate()
    }

    binding.darkModeButton.setOnClickListener {
      AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)

      activity.recreate()
    }

    val modelToRender = model.delegate
    val rendererId =
      modelToRender::class.hashCode() + model.backstackScope.lastBackstackChange.value.backstack.size
    val renderer =
      rendererFactory.getRenderer(
        modelToRender::class,
        binding.childContainer,
        rendererId,
      )
    binding.childContainer.removeAllViews()
    renderer.render(modelToRender)

    if (false) {
      val modelToRender = model.delegate
      val rendererId =
        modelToRender::class.hashCode() + model.backstackScope.lastBackstackChange.value.backstack.size
      val renderer =
        rendererFactory.getRenderer(
          modelToRender::class,
          binding.childContainer,
          rendererId,
        )

      val animationContentKey =
        model::class.hashCode() + model.backstackScope.lastBackstackChange.value.backstack.size
      val isPop =
        model.backstackScope.lastBackstackChange.value.action ==
          PresenterBackstackScope.BackstackChange.Action.POP

      if (animationContentKey != lastAnimationContentKey) {
        var oldView = binding.childContainer.getChildAt(binding.childContainer.childCount - 1)

        val slideOutAnim =
          if (isPop) {
            AnimationUtils.loadAnimation(activity, R.anim.slide_out_right)
          } else {
            AnimationUtils.loadAnimation(activity, R.anim.slide_out_left)
          }

        val slideInAnim =
          if (isPop) {
            AnimationUtils.loadAnimation(activity, R.anim.slide_in_left)
          } else {
            AnimationUtils.loadAnimation(activity, R.anim.slide_in_right)
          }

        renderer.render(modelToRender)
        val newView = binding.childContainer.getChildAt(binding.childContainer.childCount - 1)

        slideOutAnim.setAnimationListener(
          object : Animation.AnimationListener {
            override fun onAnimationEnd(p0: Animation?) {
              oldView?.let { binding.childContainer.removeView(it) }
              oldView = null
            }

            override fun onAnimationRepeat(p0: Animation?) = Unit

            override fun onAnimationStart(p0: Animation?) = Unit
          }
        )

        oldView?.startAnimation(slideOutAnim)
        newView?.startAnimation(slideInAnim)
      } else {
        renderer.render(modelToRender)
      }

      lastAnimationContentKey = animationContentKey
    }
  }
}
