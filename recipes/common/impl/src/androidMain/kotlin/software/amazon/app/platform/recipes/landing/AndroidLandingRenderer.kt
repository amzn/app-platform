package software.amazon.app.platform.recipes.landing

import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.common.impl.databinding.LandingViewBinding
import software.amazon.app.platform.renderer.ViewBindingRenderer

@ContributesRenderer
class AndroidLandingRenderer : ViewBindingRenderer<LandingPresenter.Model, LandingViewBinding>() {
  override fun inflateViewBinding(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: LandingPresenter.Model,
  ): LandingViewBinding = LandingViewBinding.inflate(layoutInflater, parent, false)

  override fun renderModel(model: LandingPresenter.Model) {

    binding.button.setOnClickListener {
      model.onEvent(LandingPresenter.Event.AddPresenterToBackstack)
    }
  }
}
