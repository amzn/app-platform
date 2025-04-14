package software.amazon.app.platform.renderer

import kotlin.reflect.KClass
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.diComponent

/**
 * Default implementation for [RendererFactory]. Implementations usually override [createRenderer]
 * and [getRenderer] to configure the [Renderer]s provided by the dependency graph further.
 */
public open class BaseRendererFactory(rootScopeProvider: RootScopeProvider) : RendererFactory {

  private val rendererComponent =
    rootScopeProvider.rootScope.diComponent<RendererComponent.Parent>().rendererComponent(this)

  private val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> = rendererComponent.renderers

  private val cacheKeys: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
    rendererComponent.modelToRendererMapping

  private val rendererCache = mutableMapOf<CacheKey, Renderer<*>>()
  private val lock = SynchronizedObject()

  override fun <T : BaseModel> createRenderer(modelType: KClass<out T>): Renderer<T> {
    @Suppress("UNCHECKED_CAST")
    return checkNotNull(renderers[modelType]?.invoke() as? Renderer<T>) {
      errorMessageForMissingRenderer(modelType)
    }
  }

  @Suppress("UNCHECKED_CAST")
  override fun <T : BaseModel> getRenderer(modelType: KClass<out T>, rendererId: Int): Renderer<T> {
    val rendererClass =
      checkNotNull(cacheKeys[modelType]) { errorMessageForMissingRenderer(modelType) }

    return synchronized(lock) {
      rendererCache.getOrPut(CacheKey(rendererClass, rendererId)) { createRenderer(modelType) }
    }
      as Renderer<T>
  }

  @Suppress("NOTHING_TO_INLINE")
  private inline fun <T : BaseModel> errorMessageForMissingRenderer(
    modelType: KClass<out T>
  ): String {
    return "No renderer was provided for $modelType. Did you add @ContributesRenderer?"
  }

  private data class CacheKey(val clazz: KClass<out Renderer<*>>, val rendererId: Int)
}
