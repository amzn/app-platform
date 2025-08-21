package software.amazon.app.platform.recipes

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import dev.zacsweers.metro.createGraph
import dev.zacsweers.metro.createGraphFactory

/** The main function to launch the Desktop app. */
fun main() {
  val desktopApp = DesktopApp {
//    DesktopAppComponent::class.create(it)
    createGraphFactory<DesktopAppComponent.Factory>().create(it)
  }

  application {
    Window(
      onCloseRequest = {
        desktopApp.destroy()
        exitApplication()
      },
      // alwaysOnTop helps during development to see the application in foreground.
      alwaysOnTop = true,
      title = "App Platform",
    ) {
      desktopApp.renderTemplates()
    }
  }
}
