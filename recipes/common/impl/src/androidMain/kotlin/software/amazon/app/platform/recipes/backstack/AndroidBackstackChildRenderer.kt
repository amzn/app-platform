package software.amazon.app.platform.recipes.backstack

import android.annotation.SuppressLint
import android.app.Activity
import android.view.LayoutInflater
import android.view.ViewGroup
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.recipes.backstack.presenter.BackstackChildPresenter
import software.amazon.app.platform.recipes.common.impl.databinding.BackstackChildViewBinding
import software.amazon.app.platform.renderer.ViewBindingRenderer

@ContributesRenderer
class AndroidBackstackChildRenderer : ViewBindingRenderer<BackstackChildPresenter.Model, BackstackChildViewBinding>() {
  override fun inflateViewBinding(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: BackstackChildPresenter.Model
  ): BackstackChildViewBinding = BackstackChildViewBinding.inflate(layoutInflater, parent, false)

  @SuppressLint("SetTextI18n")
  override fun renderModel(model: BackstackChildPresenter.Model) {
    binding.childNumberText.text = "Child: ${model.index}"
    binding.counterText.text = model.counter.toString()

    binding.button.setOnClickListener {
      model.onEvent(BackstackChildPresenter.Event.AddPresenterToBackstack)
    }
  }
}

