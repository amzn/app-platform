package software.amazon.app.platform.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project
import software.amazon.app.platform.gradle.ModuleStructureDependencyCheckTask.Companion.registerModuleStructureDependencyCheckTask

/** The Gradle plugin that sets up our module structure. */
public open class ModuleStructurePlugin : Plugin<Project> {
  override fun apply(target: Project) {
    target.ensureFollowsNamingConvention()
    target.addModuleStructureDependencies()
    target.configureAndroidNamespace()
    target.registerModuleStructureDependencyCheckTask()
  }

  private fun Project.ensureFollowsNamingConvention() {
    check(isUsingModuleStructure()) {
      "$path enables the module structure, but the project name doesn't follow the naming convention."
    }
  }

  private fun Project.addModuleStructureDependencies() {
    plugins.withIds(
      PluginIds.KOTLIN_MULTIPLATFORM,
      PluginIds.KOTLIN_JVM,
      PluginIds.KOTLIN_ANDROID,
    ) {
      val parent = requireParent()

      // Nothing to add.
      if (isPublicModule()) return@withIds

      fun addPublicModule() {
        // this is ok because no properties within publicModule are accessed
        @Suppress("GradleProjectIsolation") val publicModule = findProject("${parent.path}:public")
        if (publicModule != null) {
          if (isKmpModule) {
            dependencies.add("commonMainApi", publicModule)
          } else {
            dependencies.add("api", publicModule)
          }
        }
      }

      when {
        isTestingModule() -> {
          // :testing modules provide helper functions or fake implementations of the
          // APIs in the :public module.
          addPublicModule()
        }

        isImplModule() || isInternalModule() -> {
          // :impl and :internal modules implement interfaces and types from the :public
          // module.
          addPublicModule()
        }

        isRobotsModule() -> {
          // :robot modules usually reference types from the :public and :impl modules.
          addPublicModule()

          // Add a dependency to the implementation module. Note that an "implementation"
          // dependency is chosen rather than an "api" dependency. The goal of the a
          // robots module to hide all details of the :impl module and only expose
          // abstractions with the help of robots.
          @Suppress("GradleProjectIsolation") // no properties within project are accessed
          findProject(path.substringBefore("-robots"))
            ?.takeIf { it.isImplModule() }
            ?.let { implModule ->
              if (isKmpModule) {
                dependencies.add("commonMainImplementation", implModule)
              } else {
                dependencies.add("implementation", implModule)
              }
            }
        }
      }
    }
  }

  private fun Project.configureAndroidNamespace() {
    plugins.withIds(PluginIds.ANDROID_APP, PluginIds.ANDROID_LIBRARY) {
      // Do not override any configured namespace.
      if (android.namespace == null) {
        android.namespace = namespace()
      }
    }
  }

  public companion object {

    /**
     * Returns a consistent namespace for a Gradle module that has the recommended App Platform
     * module structure in mind. It helps to avoid clashing namespaces across projects.
     *
     * This value can be used as namespace for Android projects and gets automatically set when no
     * other namespace is declared.
     *
     * It requires that the `GROUP` property is set for the Gradle project.
     *
     * E.g. it produces following results:
     * ```
     * GROUP=software.amazon.abc
     *
     * :def:public  -> "software.amazon.abc.def"
     * :def:impl  -> "software.amazon.abc.def.impl"
     * :def:impl-ghj-robots  -> "software.amazon.abc.def.impl.ghj.robots"
     * ```
     *
     * @see com.android.build.api.dsl.CommonExtension.namespace
     */
    public fun Project.namespace(): String {
      val group =
        providers.gradleProperty("GROUP").let {
          check(it.isPresent) {
            "Couldn't find the GROUP property for this project. Make sure you define " +
              "a group in the project's gradle.properties file, e.g. `GROUP=" +
              "software.amazon.abc`."
          }
          return@let it.get()
        }

      val path =
        when {
          isPublicModule() -> requireParent().path
          isAnyPublicModule() && isRobotsModule() -> "${requireParent().path}:robots"
          else -> path
        }

      return "$group${path.replace(':', '.').replace('-', '.')}"
    }

    /**
     * Returns a consistent artifact ID for a Gradle module that has the recommended App Platform
     * module structure in mind. This artifact ID should be used for publishing library modules.
     *
     * It produces following results:
     * ```
     * :abc:public  -> "abc-public"
     * :abc:impl-def-robots  -> "abc-impl-def-robots"
     * ```
     */
    public fun Project.artifactId(libraryName: String = requireParent().name): String {
      return "$libraryName-$name"
    }

    internal val Project.testingSourceSets: List<String>
      get() = buildList {
        when {
          plugins.hasPlugin(PluginIds.KOTLIN_MULTIPLATFORM) -> {
            add("commonTest")
            if (moduleType.useTestDependenciesInMain) {
              add("commonMain")
            }
          }

          plugins.hasPlugin(PluginIds.KOTLIN_ANDROID) ||
            plugins.hasPlugin(PluginIds.KOTLIN_JVM) -> {
            add("testImplementation")
            if (moduleType.useTestDependenciesInMain) {
              add("implementation")
            }
          }
        }
      }
  }
}
