package software.amazon.app.platform.sample.user

import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * Default implementation of [AnimationHelper] that always keeps animations enabled. This
 * implementation is used for iOS and Desktop.
 */
@Inject
@ContributesBinding(AppScope::class)
class DefaultAnimationsHelper : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean = true
}
