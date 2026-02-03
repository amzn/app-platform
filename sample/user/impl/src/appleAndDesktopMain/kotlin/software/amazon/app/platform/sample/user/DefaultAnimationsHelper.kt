package software.amazon.app.platform.sample.user

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject

/**
 * Default implementation of [AnimationHelper] that always keeps animations enabled. This
 * implementation is used for iOS and Desktop.
 */
@Inject
@ContributesBinding(AppScope::class)
class DefaultAnimationsHelper : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean = true
}
