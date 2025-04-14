package software.amazon.app.platform.sample.user

/**
 * Helper class to determine whether animations are enabled. This helps with flaky UI tests.
 *
 * The interesting part is that this interface is part of the `:impl` module, because it doesn't
 * need to be shared with other modules. The implementations live in the platform specific folders
 * like `androidMain`. kotlin-inject-anvil will use the right implementation on each platform
 * automatically.
 */
interface AnimationHelper {

  /** Whether animations are enabled, e.g. on Android they're turned off during UI tests. */
  fun isAnimationsEnabled(): Boolean
}
