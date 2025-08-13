package software.amazon.app.platform.recipes

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import java.awt.Dimension
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.bunny.WindowSize

/** The main function to launch the Desktop app. */
fun main() {
  val desktopApp = DesktopApp { DesktopAppComponent::class.create(it) }

  application {
    val keyEventFlow = remember {
      MutableSharedFlow<KeyEvent>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
      )
    }

    Window(
      onCloseRequest = {
        desktopApp.destroy()
        exitApplication()
      },
      // alwaysOnTop helps during development to see the application in foreground.
      alwaysOnTop = true,
      onPreviewKeyEvent = {
        if (it.type == KeyEventType.KeyUp) {
          keyEventFlow.tryEmit(it)
        }
        false
      },
      title = "App Platform",
    ) {
      val windowSizes =
        listOf(
          Dimension(1085, 678),
          Dimension(1371, 639),
          Dimension(2248, 950),
          Dimension(678, 1085),
        )
      var windowSizeIndex by remember { mutableIntStateOf(0) }

      LaunchedEffect(Unit) {
        // Set the initial value.
        window.size = windowSizes[windowSizeIndex]

        keyEventFlow.collect {
          when (it.key) {
            Key.S -> {
              windowSizeIndex = (windowSizeIndex + 1).mod(windowSizes.size)
              window.size = windowSizes[windowSizeIndex]
            }
          }
        }
      }

      val density = LocalDensity.current
      val windowContainerSize = LocalWindowInfo.current.containerSize

      val windowSize =
        density.run {
          WindowSize.fromDimensions(
            width = windowContainerSize.width.toDp().value.toInt(),
            height = windowContainerSize.height.toDp().value.toInt(),
          )
        }

      CompositionLocalProvider(LocalWindowSize provides windowSize) {
        desktopApp.renderTemplates(keyEventFlow)
      }
    }
  }
}
