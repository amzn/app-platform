package software.amazon.app.platform.metro.compiler

import org.jetbrains.kotlin.fir.extensions.FirExtensionRegistrar
import software.amazon.app.platform.metro.compiler.fir.AppPlatformMetroExtensionsFirCheckers

public class AppPlatformMetroExtensionsPluginRegistrar : FirExtensionRegistrar() {
  override fun ExtensionRegistrarContext.configurePlugin() {
    +::AppPlatformMetroExtensionsFirCheckers
  }
}
