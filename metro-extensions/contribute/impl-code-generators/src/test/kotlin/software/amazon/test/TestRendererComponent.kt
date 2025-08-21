package software.amazon.test

import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Provider
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererScope
import kotlin.reflect.KClass

interface TestRendererComponent {
  val renderers: Map<KClass<out BaseModel>, Provider<Renderer<*>>>

  @ForScope(RendererScope::class)
  val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>>
}
