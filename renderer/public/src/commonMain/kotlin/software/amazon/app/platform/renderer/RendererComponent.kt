package software.amazon.app.platform.renderer

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import software.amazon.app.platform.presenter.BaseModel

/** Component that provides all [Renderer] instance from the dependency graph. */
@GraphExtension(RendererScope::class)
@SingleIn(RendererScope::class)
public interface RendererComponent {
  /** All [Renderer]s provided in the dependency graph. */
  public val renderers: Map<KClass<out BaseModel>, Provider<Renderer<*>>>

  /**
   * [RendererFactory]s cache renderers based on the model type. This works well, when there's a one
   * to one relationship between a model type and a renderer. However, for sealed hierarchies there
   * are multiple model types pointing to the same renderer.
   *
   * This map, given any model type as key, returns the type that should be used as key for caching.
   * For non-sealed hierarchies (aka a single model type per renderer) there is only single mapping
   * between the model and renderer class. For sealed hierarchies there are multiple entries with
   * all keys pointing to the same renderer class.
   */
  @ForScope(RendererScope::class)
  public val modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>>

  /** The parent interface to create a [RendererComponent]. */
  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  public interface Parent {
    /** Creates a new [RendererComponent]. */
    public fun rendererComponent(@Provides factory: RendererFactory): RendererComponent
  }
}
