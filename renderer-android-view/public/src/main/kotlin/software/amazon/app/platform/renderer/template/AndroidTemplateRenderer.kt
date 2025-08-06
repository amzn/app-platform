package software.amazon.app.platform.renderer.template

import android.app.Activity
import android.view.View
import android.view.ViewGroup
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.template.Template
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.ViewRenderer
import software.amazon.app.platform.renderer.getRenderer

/**
 * Base [Renderer] for Android that wraps concrete templates in a [Container] conceptionally. A
 * [Container] is backed by a [ViewGroup] and when switching between different templates the old
 * container (read `ViewGroup`) gets hidden and the new container (read `ViewGroup`) gets shown.
 */
public abstract class AndroidTemplateRenderer<T : Template>(
  private val rendererFactory: RendererFactory
) : ViewRenderer<T>() {

  protected inner class Container(
    @Suppress("unused") private val activity: Activity,
    private val viewGroup: ViewGroup,
    private val parentViewGroup: ViewGroup?,
  ) {
    private var lastRenderer: Renderer<*>? = null

    public fun renderModel(model: BaseModel) {
      show()

      val renderer = rendererFactory.getRenderer(model::class, viewGroup)
      if (renderer !== lastRenderer) {
        viewGroup.removeAllViews()
        lastRenderer = renderer
      }

      renderer.render(model)
    }

    public fun reset() {
      viewGroup.removeAllViews()
      hide()
    }

    public fun hide() {
      viewGroup.visibility = View.GONE
      parentViewGroup?.visibility = View.GONE
    }

    public fun show() {
      viewGroup.visibility = View.VISIBLE
      parentViewGroup?.visibility = View.VISIBLE
    }
  }
}
