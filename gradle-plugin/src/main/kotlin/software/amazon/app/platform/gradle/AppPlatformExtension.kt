package software.amazon.app.platform.gradle

import com.google.devtools.ksp.gradle.KspExtension
import gradle_plugin.BuildConfig.ANDROID_COMPOSE_VERSION
import gradle_plugin.BuildConfig.APP_PLATFORM_GROUP
import gradle_plugin.BuildConfig.APP_PLATFORM_VERSION
import gradle_plugin.BuildConfig.KOTLIN_INJECT_ANVIL_VERSION
import gradle_plugin.BuildConfig.KOTLIN_INJECT_VERSION
import gradle_plugin.BuildConfig.MOLECULE_VERSION
import javax.inject.Inject
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import software.amazon.app.platform.gradle.ModuleStructurePlugin.Companion.testingSourceSets

/**
 * The extension to configure the App Platform. Following options are available:
 * ```
 * appPlatform {
 *   enableKotlinInject true // false is the default
 *   enableMetro true // false is the default
 *
 *   enableMoleculePresenters true // false is the default
 *   enableModuleStructure true // false is the default
 *   enableComposeUi true // false is the default
 *
 *   addPublicModuleDependencies true // false is the default
 *   addImplModuleDependencies true // false is the default
 * }
 * ```
 */
@Suppress("TooManyFunctions", "unused")
public open class AppPlatformExtension
@Inject
constructor(objects: ObjectFactory, private val project: Project) {
  private val enableKotlinInject: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds KSP and kotlin-inject as dependency. */
  public fun enableKotlinInject(enabled: Boolean) {
    if (enabled == enableKotlinInject.get()) return

    enableKotlinInject.set(enabled)
    enableKotlinInject.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableKotlinInject()
    }
  }

  internal fun isKotlinInjectEnabled(): Property<Boolean> = enableKotlinInject

  private val enableMetro: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds Metro as dependency. */
  public fun enableMetro(enabled: Boolean) {
    if (enabled == enableMetro.get()) return

    enableMetro.set(enabled)
    enableMetro.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableMetro()
    }
  }

  internal fun isMetroEnabled(): Property<Boolean> = enableMetro

  private val enableMoleculePresenters: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds the Molecule Gradle plugin as dependency and gives access to `MoleculePresenter`. */
  public fun enableMoleculePresenters(enabled: Boolean) {
    if (enabled == enableMoleculePresenters.get()) return

    enableMoleculePresenters.set(enabled)
    enableMoleculePresenters.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableMoleculePresenters()
    }
  }

  internal fun isMoleculeEnabled(): Property<Boolean> = enableMoleculePresenters

  private val enableComposeUi: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds necessary dependencies to use Compose Multiplatform with Renderers. */
  public fun enableComposeUi(enabled: Boolean) {
    if (enabled == enableComposeUi.get()) return

    enableComposeUi.set(enabled)
    enableComposeUi.disallowChanges()

    if (enabled) {
      addPublicModuleDependencies(true)
      project.enableComposeUi()
    }
  }

  internal fun isComposeUiEnabled(): Property<Boolean> = enableComposeUi

  private val addImplModuleDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /**
   * Adds a dependency on all :impl modules. This is helpful for application modules that import all
   * implementations.
   */
  public fun addImplModuleDependencies(add: Boolean) {
    addImplModuleDependencies.set(add)
    addImplModuleDependencies.finalizeValueOnRead()

    if (add) {
      addPublicModuleDependencies(true)
    }
  }

  internal fun isAddImplModuleDependencies(): Property<Boolean> = addImplModuleDependencies

  private val addPublicModuleDependencies: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Adds dependencies on `:public` modules for the Presenters, Renderers and Scopes. */
  public fun addPublicModuleDependencies(add: Boolean) {
    addPublicModuleDependencies.set(add)
    addPublicModuleDependencies.finalizeValueOnRead()
  }

  internal fun isAddPublicModuleDependencies(): Property<Boolean> = addPublicModuleDependencies

  private val enableModuleStructure: Property<Boolean> =
    objects.property(Boolean::class.java).convention(false)

  /** Sets up this module to use our recommended module structure and applies certain defaults. */
  public fun enableModuleStructure(enable: Boolean) {
    if (enable == enableModuleStructure.get()) return

    enableModuleStructure.set(enable)
    enableModuleStructure.disallowChanges()

    if (enable) {
      project.plugins.apply(ModuleStructurePlugin::class.java)
    }
  }

  internal fun isModuleStructureEnabled(): Property<Boolean> = enableModuleStructure

  internal companion object {
    internal val Project.appPlatform: AppPlatformExtension
      get() = extensions.getByType(AppPlatformExtension::class.java)
  }
}

@Suppress("LongMethod")
private fun Project.enableKotlinInject() {
  plugins.apply(PluginIds.KSP)

  val kspExtension = extensions.getByType(KspExtension::class.java)

  // Disable this processor, because we implement our own version in order to support the
  // Scoped interface.
  kspExtension.arg(
    "software.amazon.lastmile.kotlin.inject.anvil.processor.ContributesBindingProcessor",
    "disabled",
  )

  fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
    add(kspConfigurationName, "me.tatarka.inject:kotlin-inject-compiler-ksp:$KOTLIN_INJECT_VERSION")
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION",
    )
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-impl-code-generators:$APP_PLATFORM_VERSION",
    )
    add(
      kspConfigurationName,
      "software.amazon.lastmile.kotlin.inject.anvil:compiler:$KOTLIN_INJECT_ANVIL_VERSION",
    )
  }

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("me.tatarka.inject:kotlin-inject-runtime:$KOTLIN_INJECT_VERSION")
      implementation("$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:kotlin-inject-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION")
      implementation(
        "software.amazon.lastmile.kotlin.inject.anvil:runtime:$KOTLIN_INJECT_ANVIL_VERSION"
      )
      implementation(
        "software.amazon.lastmile.kotlin.inject.anvil:runtime-optional:" +
          KOTLIN_INJECT_ANVIL_VERSION
      )
    }

    kmpExtension.targets.configureEach { target ->
      addKspDependenciesWhenConfigExists(target) { configName ->
        dependencies.addKspProcessorDependencies(configName)
      }
    }
  }

  plugins.withIds(PluginIds.KOTLIN_ANDROID, PluginIds.KOTLIN_JVM) {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:kotlin-inject-public:$APP_PLATFORM_VERSION",
    )
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:kotlin-inject-contribute-public:$APP_PLATFORM_VERSION",
    )
    dependencies.add(
      "implementation",
      "software.amazon.lastmile.kotlin.inject.anvil:runtime:$KOTLIN_INJECT_ANVIL_VERSION",
    )
    dependencies.add(
      "implementation",
      "software.amazon.lastmile.kotlin.inject.anvil:runtime-optional:$KOTLIN_INJECT_ANVIL_VERSION",
    )
    dependencies.add(
      "implementation",
      "me.tatarka.inject:kotlin-inject-runtime:$KOTLIN_INJECT_VERSION",
    )
    dependencies.addKspProcessorDependencies("ksp")
  }
}

private fun Project.enableMetro() {
  plugins.apply(PluginIds.METRO)

  // Enable KSP for our custom extensions.
  plugins.apply(PluginIds.KSP)

  fun DependencyHandler.addKspProcessorDependencies(kspConfigurationName: String) {
    add(
      kspConfigurationName,
      "$APP_PLATFORM_GROUP:metro-contribute-impl-code-generators:$APP_PLATFORM_VERSION",
    )
  }

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")
      implementation("$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")

      kmpExtension.targets.configureEach { target ->
        addKspDependenciesWhenConfigExists(target) { configName ->
          dependencies.addKspProcessorDependencies(configName)
        }
      }
    }
  }

  plugins.withIds(PluginIds.KOTLIN_ANDROID, PluginIds.KOTLIN_JVM) {
    dependencies.add("implementation", "$APP_PLATFORM_GROUP:di-common-public:$APP_PLATFORM_VERSION")

    dependencies.add("implementation", "$APP_PLATFORM_GROUP:metro-public:$APP_PLATFORM_VERSION")

    dependencies.addKspProcessorDependencies("ksp")
  }
}

private fun Project.enableMoleculePresenters() {
  plugins.apply(PluginIds.COMPOSE_COMPILER)

  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation("app.cash.molecule:molecule-runtime:$MOLECULE_VERSION")
      implementation("$APP_PLATFORM_GROUP:presenter-molecule-public:$APP_PLATFORM_VERSION")
    }
    testingSourceSets.forEach { sourceSetName ->
      kmpExtension.sourceSets.getByName(sourceSetName).dependencies {
        implementation("$APP_PLATFORM_GROUP:presenter-molecule-testing:$APP_PLATFORM_VERSION")
      }
    }
  }

  plugins.withIds(PluginIds.KOTLIN_ANDROID, PluginIds.KOTLIN_JVM) {
    dependencies.add("implementation", "app.cash.molecule:molecule-runtime:$MOLECULE_VERSION")
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:presenter-molecule-public:$APP_PLATFORM_VERSION",
    )
    testingSourceSets.forEach { sourceSetName ->
      dependencies.add(
        sourceSetName,
        "$APP_PLATFORM_GROUP:presenter-molecule-testing:$APP_PLATFORM_VERSION",
      )
    }
  }
}

private fun Project.enableComposeUi() {
  plugins.withId(PluginIds.KOTLIN_MULTIPLATFORM) {
    plugins.apply(PluginIds.COMPOSE_COMPILER)
    plugins.apply(PluginIds.COMPOSE_MULTIPLATFORM)

    kmpExtension.sourceSets.getByName("commonMain").dependencies {
      implementation(composeDependencies.foundation)
      implementation(composeDependencies.runtime)

      implementation(
        "$APP_PLATFORM_GROUP:renderer-compose-multiplatform-public:$APP_PLATFORM_VERSION"
      )

      if (isRobotsModule()) {
        implementation(
          "$APP_PLATFORM_GROUP:robot-compose-multiplatform-public:$APP_PLATFORM_VERSION"
        )
      }
    }
  }

  plugins.withIds(PluginIds.KOTLIN_ANDROID) {
    plugins.apply(PluginIds.COMPOSE_COMPILER)

    android.buildFeatures.compose = true

    dependencies.add("implementation", "androidx.compose.runtime:runtime:$ANDROID_COMPOSE_VERSION")
    dependencies.add(
      "implementation",
      "androidx.compose.foundation:foundation:$ANDROID_COMPOSE_VERSION",
    )
    dependencies.add(
      "implementation",
      "$APP_PLATFORM_GROUP:renderer-compose-multiplatform-public:$APP_PLATFORM_VERSION",
    )

    if (isRobotsModule()) {
      dependencies.add(
        "implementation",
        "$APP_PLATFORM_GROUP:robot-compose-multiplatform-public:$APP_PLATFORM_VERSION",
      )
    }
  }

  plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
    android.buildFeatures.compose = true

    if (isAppModule()) {
      dependencies.add(
        "androidTestImplementation",
        "$APP_PLATFORM_GROUP:robot-compose-multiplatform-public:$APP_PLATFORM_VERSION",
      )
    }
  }
}

private fun Project.addKspDependenciesWhenConfigExists(
  target: KotlinTarget,
  block: (String) -> Unit,
) {
  if (target.name != "metadata") {
    target.compilations.configureEach { compilation ->
      fun configExists(name: String): Boolean = configurations.any { it.name == name }

      // The implementationConfigurationName name is
      // 'iosSimulatorArm64CompilationImplementation', 'wasmJsTestCompileClasspath' or
      // 'desktopCompileClasspath'.
      //
      // E.g. 'desktopCompileClasspath' with give use 'kspDesktop'
      var configName =
        "ksp" +
          compilation.implementationConfigurationName.substringBefore("Compilation").capitalize()

      if (!configExists(configName) && target.platformType == KotlinPlatformType.androidJvm) {
        // Android has different naming for some reason.
        //
        // E.g. for instrumentation tests 'kspAndroidDebugAndroidTest' should actually be
        // 'kspAndroidAndroidTestDebug', but we will use 'kspAndroidAndroidTest'.
        //
        // For unit tests 'kspAndroidDebugUnitTest' should actually be 'kspAndroidTestDebug',
        // but we will use 'kspAndroidTest'.
        when {
          configName.endsWith("AndroidTest") -> configName = "kspAndroidAndroidTest"
          configName.endsWith("UnitTest") -> configName = "kspAndroidTest"
        }
      }

      // Check again if the config exists.
      if (configExists(configName)) {
        block(configName)
      }
    }
  }
}
