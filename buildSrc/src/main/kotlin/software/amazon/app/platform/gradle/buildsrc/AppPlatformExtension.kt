package software.amazon.app.platform.gradle.buildsrc

import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import software.amazon.app.platform.gradle.AppPlatformExtension as AppPlatformExtensionGradlePlugin
import software.amazon.app.platform.gradle.buildsrc.BaseAndroidPlugin.Companion.enableInstrumentedTests
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.enableCompose
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.enableDi
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.enableMetro
import software.amazon.app.platform.gradle.buildsrc.KmpPlugin.Companion.enableMolecule
import software.amazon.app.platform.gradle.buildsrc.SdkPlugin.publishSdk

@Suppress("unused")
public open class AppPlatformExtension
@Inject
constructor(objects: ObjectFactory, private val project: Project) {
  private val enableCompose: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  public fun enableCompose(enabled: Boolean) {
    enableCompose.set(enabled)
    enableCompose.disallowChanges()

    if (enabled) {
      project.enableCompose()
    }
  }

  internal fun isComposeEnabled(): Property<Boolean> = enableCompose

  private val enableDi: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  public fun enableDi(enabled: Boolean) {
    enableDi.set(enabled)
    enableDi.disallowChanges()

    if (enabled) {
      project.enableDi()
    }
  }

  internal fun isDiEnabled(): Property<Boolean> = enableDi

  private val enableMetro: Property<Boolean> = objects.property(Boolean::class.java).convention(false)

  public fun enableMetro(enabled: Boolean) {
    enableMetro.set(enabled)
    enableMetro.disallowChanges()

    if (enabled) {
      project.enableMetro()
    }
  }

  internal fun isMetroEnabled(): Property<Boolean> = enableMetro

  private val enableMolecule: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  public fun enableMolecule(enabled: Boolean) {
    enableMolecule.set(enabled)
    enableMolecule.disallowChanges()

    if (enabled) {
      project.enableMolecule()
    }
  }

  internal fun isMoleculeEnabled(): Property<Boolean> = enableMolecule

  private val enablePublishing: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  public fun enablePublishing(enabled: Boolean) {
    enablePublishing.set(enabled)
    enablePublishing.disallowChanges()

    if (enabled) {
      project.publishSdk()
    }
  }

  internal fun isPublishingEnabled(): Property<Boolean> = enablePublishing

  private val kotlinWarningsAsErrors: Property<Boolean> =
    objects
      .property(Boolean::class.java)
      .convention(
        project.provider {
          val useKsp2 =
            project.providers.gradleProperty("ksp.useKSP2").map { it.toBooleanStrict() }.get()

          if (useKsp2) {
            project.ci || project.gradle.taskGraph.hasTask("${project.path}:release")
          } else {
            // With KSP1 the Kotlin compiler produces warnings. We still want to test KSP1,
            // therefore warnings should not be errors in this mode.
            false
          }
        }
      )

  public fun kotlinWarningsAsErrors(enabled: Boolean) {
    kotlinWarningsAsErrors.set(enabled)
    kotlinWarningsAsErrors.finalizeValueOnRead()
  }

  internal fun isKotlinWarningsAsErrors(): Property<Boolean> = kotlinWarningsAsErrors

  private val enableInstrumentedTests: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  public fun enableInstrumentedTests(enabled: Boolean) {
    if (enableInstrumentedTests.get() == enabled) {
      return
    }

    enableInstrumentedTests.set(enabled)
    enableInstrumentedTests.disallowChanges()

    if (enabled) {
      project.enableInstrumentedTests()
    }
  }

  internal fun isInstrumentedTestsEnabled(): Property<Boolean> = enableInstrumentedTests

  internal companion object {
    val Project.appPlatformBuildSrc: AppPlatformExtension
      get() = extensions.getByType(AppPlatformExtension::class.java)

    val Project.appPlatformGradlePlugin: AppPlatformExtensionGradlePlugin
      get() = extensions.getByType(AppPlatformExtensionGradlePlugin::class.java)
  }
}
