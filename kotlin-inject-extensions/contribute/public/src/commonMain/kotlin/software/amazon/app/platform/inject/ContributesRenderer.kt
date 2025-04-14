package software.amazon.app.platform.inject

import kotlin.annotation.AnnotationTarget.CLASS
import kotlin.reflect.KClass
import software.amazon.lastmile.kotlin.inject.anvil.extend.ContributingAnnotation

/**
 * Used to contribute a renderer to our global registry of renderers that can be looked up by our
 * runtime. E.g. given this renderer:
 * ```
 * @ContributesRenderer
 * class IncrementRenderer : Renderer<IncrementPresenter.Model>()
 * ```
 *
 * This annotation would generated following component interface:
 * ```
 * @ContributesTo(RendererScope::class)
 * interface IncrementRendererComponent {
 *     @Provides
 *     @IntoMap
 *     fun provideIncrementRendererIncrementPresenterModel(
 *         renderer: () -> IncrementRenderer,
 *     ): Pair<KClass<out BaseModel>, () -> Renderer<*>> = IncrementPresenter.Model::class to renderer
 *
 *     @Provides
 *     fun provideIncrementRenderer(): IncrementRenderer = IncrementRenderer()
 * }
 * ```
 *
 * Although strongly discouraged, your renderer is allowed to have an `@Inject constructor`. The
 * only valid use case is for injecting other renderers.
 *
 * ```
 * @Inject
 * @ContributesRenderer
 * class IncrementRenderer(
 *     private val otherRenderer: OtherRenderer
 * ) : Renderer<IncrementPresenter.Model>() {
 * ```
 *
 * In this case following module would be generated:
 * ```
 * @ContributesTo(RendererScope::class)
 * abstract class IncrementRendererModule {
 *     @Provides
 *     @IntoMap
 *     fun provideIncrementRendererIntoMap(
 *         renderer: () -> IncrementRenderer,
 *     ): Pair<KClass<out BaseModel>, () -> Renderer<*>> = IncrementRenderer.Model::class to renderer
 * }
 * ```
 *
 * If the model type is a sealed hierarchy, then for each explicit type a binding method will be
 * generated.
 */
@Target(CLASS)
@ContributingAnnotation
public annotation class ContributesRenderer(
  /**
   * The class reference to the model class. Usually, it doesn't need to be specified and can be
   * implied by the super type of the renderer.
   */
  val modelType: KClass<*> = Unit::class,

  /**
   * If the `Model` class is a sealed hierarchy and this value is `true` (the default), then this
   * renderer will be responsible for rendering all other sealed subtypes as well.
   */
  val includeSealedSubtypes: Boolean = true,
)
