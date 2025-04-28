package software.amazon.app.platform.sample

import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.sample.user.AnimationHelper
import software.amazon.app.platform.sample.user.DefaultAnimationsHelper
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding

/**
 * This implementation replaces [DefaultAnimationsHelper] in UI tests to disable animations and make
 * tests more stable.
 */
@Inject
@ContributesBinding(AppScope::class, replaces = [DefaultAnimationsHelper::class])
class TestAnimationHelper : AnimationHelper {
  override fun isAnimationsEnabled(): Boolean = false
}
