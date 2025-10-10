package software.amazon.app.platform.recipes

import android.app.Activity
import android.app.Application
import android.os.Build
import android.os.Bundle
import android.view.Display
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.currentComposer
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.Scoped
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Provides the ability to save and restore instance state in the application scope independent
 * of an Android Activity. The underlying implementation will still rely on an Activity, but
 * callbacks are exposed to a higher scope.
 */
interface SavedInstanceStateRegistry {
  /**
   * Register [saveState] to be invoked, when the instance state must be saved. Call [unregister]
   * with the same [key] to remove the callback and avoid leaking [saveState].
   */
  fun register(
    key: String,
    saveState: Bundle.() -> Unit,
  )

  /**
   * Removes the callback that was registered in [register]. Does nothing if no such callback
   * exists.
   */
  fun unregister(key: String)

  /**
   * Consumes the saved instance state for the given [key] after the process has been recreated.
   * If no instance state has been saved for this [key], then `null` is returned. The saved
   * instance state is only returned once and then removed. Consecutive calls to this function
   * with the same [key] will always return `null`.
   */
  fun consumeRestoredState(key: String): Bundle?

  /**
   * Similar to [consumeRestoredState], but can be invoked before the actual Activity is restored
   * to avoid race conditions, e.g. in the `Application.onCreate()`.
   */
  suspend fun consumeRestoredStateDelayed(key: String): Bundle?
}

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class SavedInstanceStateRegistryImpl(
  private val application: Application,
) : SavedInstanceStateRegistry, Scoped {
  private val restoredInstanceState = MutableStateFlow<Bundle?>(null)
  private val providers = mutableMapOf<String, Bundle.() -> Unit>()

  private val activityListener = object : Application.ActivityLifecycleCallbacks {
    private val createdActivities = mutableListOf<Activity>()

    override fun onActivityCreated(
      activity: Activity,
      savedInstanceState: Bundle?,
    ) {
      if (!activity.isDefaultDisplay()) return

      createdActivities += activity

      if (restoredInstanceState.value == null) {
        // Only restore the instance state AFTER the process has been recreated and never
        // again.
        restoredInstanceState.value = savedInstanceState?.deepCopy() ?: Bundle.EMPTY
      }
    }

    override fun onActivityStarted(activity: Activity) = Unit

    override fun onActivityResumed(activity: Activity) = Unit

    override fun onActivityPaused(activity: Activity) = Unit

    override fun onActivityStopped(activity: Activity) = Unit

    override fun onActivitySaveInstanceState(
      activity: Activity,
      outState: Bundle,
    ) {
      if (!activity.isDefaultDisplay()) return

      if (activity == createdActivities.last()) {
        // Only save the instance state for the last created activity, because it sits
        // in the activity stack on top and will be the one that will be restored first.
        providers.forEach { (key, saveState) ->
          outState.putBundle(key, Bundle().apply(saveState))
        }
      }
    }

    override fun onActivityDestroyed(activity: Activity) {
      if (!activity.isDefaultDisplay()) return

      createdActivities -= activity
    }
  }

  override fun onEnterScope(scope: Scope) {
    application.registerActivityLifecycleCallbacks(activityListener)
  }

  override fun onExitScope() {
    application.unregisterActivityLifecycleCallbacks(activityListener)
  }

  override fun register(
    key: String,
    saveState: Bundle.() -> Unit,
  ) {
    providers[key] = saveState
  }

  override fun unregister(key: String) {
    providers.remove(key)
  }

  override fun consumeRestoredState(key: String): Bundle? {
    return restoredInstanceState.value?.consumeBundle(key)
  }

  override suspend fun consumeRestoredStateDelayed(key: String): Bundle? {
    return restoredInstanceState.filterNotNull().first().consumeBundle(key)
  }

  private fun Bundle.consumeBundle(key: String): Bundle? {
    return getBundle(key).also {
      remove(key)
    }
  }

  @Suppress("MagicNumber")
  private val Activity.displayId: Int
    get() {
      if (Build.VERSION.SDK_INT >= 30) {
        display?.displayId?.let { return it }
      }

      @Suppress("DEPRECATION")
      return windowManager.defaultDisplay.displayId
    }

  private fun Activity.isDefaultDisplay(): Boolean = displayId == Display.DEFAULT_DISPLAY
}

/**
 * Used to save and restore instance state within a [MoleculePresenter]. [saveState] might be
 * called multiple times while the presenter is in composition. After the process has been
 * recreated and state has been saved prior, then this function will return the restored [Bundle]
 * during the first composition. Following compositions will return `null`.
 */
@Composable
fun MoleculePresenter<*, *>.rememberInstanceState(
  key: String = this::class.java.canonicalName ?: error("Couldn't get class name"),
  savedInstanceStateRegistry: SavedInstanceStateRegistry = LocalSavedInstanceStateRegistry.current,
  saveState: Bundle.() -> Unit,
): Bundle? {
  if (currentComposer.inserting) {
    savedInstanceStateRegistry.register(key, saveState)
  }

  DisposableEffect(key) {
    onDispose {
      savedInstanceStateRegistry.unregister(key)
    }
  }

  return savedInstanceStateRegistry.consumeRestoredState(key)
}

val LocalSavedInstanceStateRegistry = staticCompositionLocalOf<SavedInstanceStateRegistry> {
  error("Missing default value for SavedInstanceStateRegistry")
}
