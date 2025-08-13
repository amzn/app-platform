package software.amazon.app.platform.bunny

import androidx.compose.runtime.compositionLocalOf

enum class WindowSize {
  Compact,
  Medium,
  Large,
  Portrait;

  companion object {
    fun fromDimensions(width: Int, height: Int): WindowSize {
      return when {
        height > width -> Portrait
        width <= 1199 -> Compact
        width <= 1899 -> Medium
        else -> Large
      }
    }
  }
}

val LocalWindowSize = compositionLocalOf { WindowSize.Compact }
