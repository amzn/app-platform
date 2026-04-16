package software.amazon.app.platform.metro.compiler.runners

import org.jetbrains.kotlin.config.JvmTarget
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.directives.CodegenTestDirectives
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.runners.codegen.AbstractFirBlackBoxCodegenTestBase
import org.jetbrains.kotlin.test.services.EnvironmentBasedStandardLibrariesPathProvider
import org.jetbrains.kotlin.test.services.KotlinStandardLibrariesPathProvider
import software.amazon.app.platform.metro.compiler.services.configureKotlinTestImports
import software.amazon.app.platform.metro.compiler.services.configureMetroImports
import software.amazon.app.platform.metro.compiler.services.configurePlugin

open class AbstractBoxTest : AbstractFirBlackBoxCodegenTestBase(FirParser.LightTree) {
  override fun createKotlinStandardLibrariesPathProvider(): KotlinStandardLibrariesPathProvider {
    return EnvironmentBasedStandardLibrariesPathProvider
  }

  override fun configure(builder: TestConfigurationBuilder) =
    with(builder) {
      super.configure(this)

      defaultDirectives {
        JvmEnvironmentConfigurationDirectives.JVM_TARGET.with(JvmTarget.JVM_11)
        +ConfigurationDirectives.WITH_STDLIB
        +JvmEnvironmentConfigurationDirectives.FULL_JDK
        +CodegenTestDirectives.IGNORE_DEXING
      }

      configurePlugin()
      configureMetroImports()
      configureKotlinTestImports()
    }
}
