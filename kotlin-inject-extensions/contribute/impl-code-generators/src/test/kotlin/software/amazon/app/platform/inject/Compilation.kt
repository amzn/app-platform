@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject

import app_platform.kotlin_inject_extensions.contribute.impl_code_generators.TestBuildConfig.USE_KSP_2
import assertk.assertThat
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation
import com.tschuchort.compiletesting.SourceFile
import com.tschuchort.compiletesting.addPreviousResultToClasspath
import com.tschuchort.compiletesting.configureKsp
import java.io.File
import java.io.OutputStream
import java.nio.file.Files
import java.util.ServiceLoader
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.jetbrains.kotlin.config.JvmTarget

/** A simple API over a [KotlinCompilation] with extra configuration support for KSP processors. */
// Inspired by Anvil:
// https://github.com/square/anvil/blob/97e2cc0430311c6b0ed5341da95bb243b582fab8/compiler-utils/src/testFixtures/java/com/squareup/anvil/compiler/internal/testing/AnvilCompilation.kt
class Compilation internal constructor(val kotlinCompilation: KotlinCompilation) {

  private var isCompiled = false
  private var processorsConfigured = false

  /** Configures the behavior of this compilation. */
  fun configureAppPlatformProcessor(): Compilation = apply {
    checkNotCompiled()
    check(!processorsConfigured) { "Processor should not be configured twice." }

    processorsConfigured = true

    val useKsp2 = USE_KSP_2

    if (!useKsp2) {
      kotlinCompilation.languageVersion = "1.9"
      kotlinCompilation.allWarningsAsErrors = false
    }

    kotlinCompilation.configureKsp(useKsp2 = useKsp2) {
      symbolProcessorProviders +=
        ServiceLoader.load(
          SymbolProcessorProvider::class.java,
          SymbolProcessorProvider::class.java.classLoader,
        )

      processorOptions +=
        "software.amazon.lastmile.kotlin.inject.anvil.processor." + "ContributesBindingProcessor" to
          "disabled"

      // Run KSP embedded directly within this kotlinc invocation
      withCompilation = true
      incremental = true
    }
  }

  /** Adds the given sources to this compilation with their packages and names inferred. */
  fun addSources(@Language("kotlin") vararg sources: String): Compilation = apply {
    checkNotCompiled()
    kotlinCompilation.sources +=
      sources.mapIndexed { index, content ->
        val packageDir =
          content
            .lines()
            .firstOrNull { it.trim().startsWith("package ") }
            ?.substringAfter("package ")
            ?.replace('.', '/')
            ?.let { "$it/" } ?: ""

        val name =
          "${kotlinCompilation.workingDir.absolutePath}/sources/src/main/java/" +
            "$packageDir/Source$index.kt"

        Files.createDirectories(File(name).parentFile.toPath())

        SourceFile.kotlin(name, contents = content, trimIndent = true)
      }
  }

  fun addPreviousCompilationResult(result: JvmCompilationResult): Compilation = apply {
    checkNotCompiled()
    kotlinCompilation.addPreviousResultToClasspath(result)
  }

  private fun checkNotCompiled() {
    check(!isCompiled) {
      "Already compiled! Create a new compilation if you want to compile again."
    }
  }

  /**
   * Compiles the underlying [KotlinCompilation]. Note that if [configureAppPlatformProcessor] has
   * not been called prior to this, it will be configured with default behavior.
   */
  fun compile(
    @Language("kotlin") vararg sources: String,
    block: JvmCompilationResult.() -> Unit = {},
  ): JvmCompilationResult {
    checkNotCompiled()
    if (!processorsConfigured) {
      // Configure with default behaviors
      configureAppPlatformProcessor()
    }
    addSources(*sources)
    isCompiled = true

    return kotlinCompilation.compile().apply(block)
  }

  companion object {
    operator fun invoke(): Compilation {
      return Compilation(
        KotlinCompilation().apply {
          // Sensible default behaviors
          inheritClassPath = true
          jvmTarget = JvmTarget.JVM_1_8.description
          verbose = false
        }
      )
    }
  }
}

/**
 * Helpful for testing code generators in unit tests end to end.
 *
 * This covers common cases, but is built upon reusable logic in [Compilation] and
 * [Compilation.configureAppPlatformProcessor]. Consider using those APIs if more advanced
 * configuration is needed.
 */
fun compile(
  @Language("kotlin") vararg sources: String,
  allWarningsAsErrors: Boolean = true,
  messageOutputStream: OutputStream = System.out,
  workingDir: File? = null,
  previousCompilationResult: JvmCompilationResult? = null,
  moduleName: String? = null,
  exitCode: KotlinCompilation.ExitCode = KotlinCompilation.ExitCode.OK,
  block: JvmCompilationResult.() -> Unit = {},
): JvmCompilationResult {
  return Compilation()
    .apply {
      kotlinCompilation.apply {
        this.allWarningsAsErrors = allWarningsAsErrors
        this.messageOutputStream = messageOutputStream
        if (workingDir != null) {
          this.workingDir = workingDir
        }
        if (moduleName != null) {
          this.moduleName = moduleName
        }
      }

      if (previousCompilationResult != null) {
        addPreviousCompilationResult(previousCompilationResult)
      }
    }
    .configureAppPlatformProcessor()
    .compile(*sources)
    .also {
      if (exitCode == KotlinCompilation.ExitCode.OK) {
        assertThat(it.exitCode).isOk()
      } else {
        assertThat(it.exitCode).isError()
      }
    }
    .also(block)
}
