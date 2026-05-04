package software.amazon.test

import dev.zacsweers.metro.ForScope
import kotlin.reflect.KClass
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererScope

interface TestRendererGraph {
  val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>>

  @ForScope(RendererScope::class)
  val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>>
}
