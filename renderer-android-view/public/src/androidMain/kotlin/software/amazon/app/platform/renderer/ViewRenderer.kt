package software.amazon.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import androidx.core.view.doOnAttach
import androidx.core.view.doOnDetach
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.coroutine.MainCoroutineDispatcher
import software.amazon.app.platform.scope.di.diComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * An implementation of [Renderer] that is specific to Android View based UI. This renderer provides
 * some built in convenience for rendering Android Views such as managing your child's UI in
 * relation to the provided parent's lifecycle.
 *
 * Your custom renderers should extend this abstract class and provide implementations for [inflate]
 * and [renderModel].
 *
 * [inflate] is where one time view set up should take place. This method will not be called again
 * while the Renderer is in use. This method is roughly equivalent to `onCreateView` and should be
 * used inflate layouts or write UI code directly in Kotlin.
 *
 * ```
 *  private lateinit var myView: TextView
 *
 *  override fun inflate(
 *      activity: Activity,
 *      parent: ViewGroup,
 *      layoutInflater: LayoutInflater
 *  ): View {
 *      return layoutInflator.inflate(R.layout.myView, parent, false).also {
 *          // Here you can initialize other variables you need to use in your [renderModel] method.
 *          myView = it.findViewById(R.id.myView)
 *      }
 *  }
 * ```
 *
 * [renderModel] is called each time there is a new model for this renderer to render. Place logic
 * here that is specific to binding model data or callbacks to your view layer.
 *
 * ```
 * override fun renderModel(model: MyModel) {
 *     myView.text = model.text
 * }
 * ```
 *
 * Optionally your custom renderer can override [onDetach] to perform cleanup tasks before your
 * views are removed from the hierarchy. Once the inflated view was detached from the window, it
 * will be removed from the parent and nullified. If this [ViewRenderer] is reused, then a new view
 * will be inflated and added to the parent.
 */
public abstract class ViewRenderer<in ModelT : BaseModel> : BaseAndroidViewRenderer<ModelT> {

  protected lateinit var activity: Activity
    private set

  protected lateinit var coroutineScope: CoroutineScope
    private set

  private lateinit var parent: ViewGroup
  private lateinit var inflater: LayoutInflater

  private var view: View? = null
  private var lastModel: ModelT? = null

  final override fun init(activity: Activity, parent: ViewGroup) {
    // Renderer is not assigned an initialized Activity or Parent has changed
    if (!this::activity.isInitialized || this.parent != parent) {
      this.activity = activity
      this.parent = parent
      inflater = activity.layoutInflater
    }
  }

  private fun createView(model: ModelT): View {
    val rootScopeProvider = activity.application as RootScopeProvider
    coroutineScope =
      CoroutineScope(rootScopeProvider.rootScope.diComponent<Component>().dispatcher + Job())
    return inflate(activity, parent, inflater, model).also { view = it }
  }

  private fun resetView() {
    coroutineScope.cancel()

    // Remove the view from the parent. In case the Renderer is reused we inflate a new
    // View and add it to the parent.
    view?.let {
      try {
        parent.removeView(it)
      } catch (_: NullPointerException) {
        // This shouldn't happen, yet it does sporadically.
        // Specifically:
        // java.lang.NullPointerException: Attempt to write to field 'android.view.ViewParent
        // android.view.View.mParent'
        //                                 on a null object reference
        // at android.view.ViewGroup.removeFromArray(ViewGroup.java:5372)
        // at android.view.ViewGroup.removeViewInternal(ViewGroup.java:5569)
        // at android.view.ViewGroup.removeViewInternal(ViewGroup.java:5531)
        // at android.view.ViewGroup.removeView(ViewGroup.java:5462)
        // at software.amazon.app.platform.renderer.ViewRenderer.resetView(ViewRenderer.kt:101)
      }
    }

    // Allows us to reclaim the memory.
    view = null
  }

  final override fun render(model: ModelT) {
    val view = view ?: createView(model)

    if (parent.children.none { it === view }) {
      parent.addView(view)

      view.doOnAttach {
        lastModel = null
        // Wait for the view to be attached first, otherwise doOnDetach gets
        // called immediately.
        view.doOnDetach {
          if (releaseViewOnDetach()) {
            // call onDetach first so view is still available during cleanup tasks
            onDetach()
            resetView()
          }
        }
      }
    }

    // Avoid re-rendering same model
    if (model == lastModel) {
      return
    }

    renderModel(model, lastModel)
    lastModel = model
  }

  /**
   * Perform one time view inflation for the layout of this Renderer. This method is called on
   * initial Render and won't be called again; it is therefore a good place to do initial setup that
   * need only be done a single time.
   */
  protected abstract fun inflate(
    activity: Activity,
    parent: ViewGroup,
    layoutInflater: LayoutInflater,
    initialModel: ModelT,
  ): View

  /**
   * Called when a new Model is given to the Renderer, use if there is no need to compare [model]
   * with any prior model in order to render specific views. This function will not be called if
   * `renderModel(model: T, lastModel: T?)` is overridden.
   */
  protected open fun renderModel(model: ModelT): Unit = Unit

  /**
   * Called when a new Model is given to the Renderer. Provides an opportunity for a Renderer to
   * compare [model] with [lastModel]. By default, this calls `renderModel(model: T)`.
   */
  protected open fun renderModel(model: ModelT, lastModel: ModelT?): Unit = renderModel(model)

  /**
   * Inheritors of this class can optionally override this method to do cleanup tasks before their
   * views are removed from the hierarchy.
   */
  public open fun onDetach(): Unit = Unit

  /**
   * This function is called when the view inflated by this renderer was detached, meaning the view
   * itself or one of its parents got removed from the view hierarchy.
   *
   * When this function returns `true`, then the view is manually removed from the parent and
   * released, meaning it can be garbage collected. [coroutineScope] is canceled at the same time.
   * The next time [render] is called, [inflate] will be called, a new view gets inflated and added
   * to the parent.
   *
   * When this function returns `false`, then the view is cached and will not be removed from the
   * parent. [coroutineScope] isn't canceled either. This behavior is usually desired in view groups
   * that manually manage their children, such as a [RecyclerView].
   *
   * The default implementation of this function checks whether any of the parent views is an
   * instance of [RecyclerView] and returns the appropriate result. This behavior can be changed by
   * subclasses of [ViewRenderer].
   */
  protected open fun releaseViewOnDetach(): Boolean {
    val parents =
      generateSequence(parent) { viewGroup -> viewGroup.parent as? ViewGroup }
        .takeWhile { it.id != android.R.id.content }

    return parents.none { it is RecyclerView }
  }

  /** DI component that provides objects from the dependency graph. */
  @ContributesTo(AppScope::class)
  public interface Component {
    /** The coroutine dispatcher using the main thread. */
    @MainCoroutineDispatcher public val dispatcher: CoroutineDispatcher
  }
}
