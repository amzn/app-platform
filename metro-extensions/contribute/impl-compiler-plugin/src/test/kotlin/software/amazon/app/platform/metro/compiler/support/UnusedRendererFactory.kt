package software.amazon.app.platform.metro.compiler.support

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory

object UnusedRendererFactory : RendererFactory {
  override fun <T : BaseModel> createRenderer(
    modelType: kotlin.reflect.KClass<out T>
  ): Renderer<T> {
    error("unused")
  }

  override fun <T : BaseModel> getRenderer(
    modelType: kotlin.reflect.KClass<out T>,
    rendererId: Int,
  ): Renderer<T> {
    error("unused")
  }
}
