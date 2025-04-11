package software.amazon.app.platform.sample

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

/** The main function to launch the Desktop app. */
fun main() {
  val desktopApp = DesktopApp { DesktopAppComponent::class.create(it) }

  application {
    Window(
      onCloseRequest = {
        desktopApp.destroy()
        exitApplication()
      },
      title = "LastMile App Platform",
    ) {
      desktopApp.renderTemplates()
    }
  }
}
