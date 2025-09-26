package software.amazon.app.platform.recipes.template

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.recipes.common.impl.R
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.template.AndroidTemplateRenderer

/**
 * A view binding renderer implementation for templates used in the recipes application.
 *
 * [rendererFactory] is used to get the [Renderer] for the [BaseModel] wrapped in the template.
 */
@Inject
@ContributesRenderer
class AndroidRecipesTemplateRenderer(private val rendererFactory: RendererFactory) :
  AndroidTemplateRenderer<RecipesAppTemplate>(rendererFactory) {

  private lateinit var fullscreenContainer: Container

  override fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: RecipesAppTemplate,
  ): View {
    return layoutInflater.inflate(R.layout.root_container, parent, false).also {
      val rootView = it as FrameLayout
      fullscreenContainer =
        Container(activity, rootView.findViewById(R.id.full_screen_container), null)
    }
  }

  override fun renderModel(model: RecipesAppTemplate) {
    when (model) {
      is RecipesAppTemplate.FullScreenTemplate -> {
        fullscreenContainer.renderModel(model.model)
      }
    }
  }
}
