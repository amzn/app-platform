@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject.metro.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.app.platform.inject.metro.compile
import software.amazon.app.platform.inject.metro.declaredNonSyntheticMethods
import software.amazon.app.platform.inject.metro.graphInterface
import software.amazon.app.platform.inject.metro.newMetroGraph
import software.amazon.app.platform.ksp.capitalize
import software.amazon.app.platform.ksp.isAnnotatedWith
import software.amazon.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.amazon.app.platform.renderer.metro.RobotKey
import software.amazon.app.platform.robot.Robot
import software.amazon.test.TestRobotGraph

class ContributesRobotGeneratorTest {

  @Test
  fun `a graph interface is generated without @Inject constructor`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      @ContributesRobot(AppScope::class)
      class TestRobot : Robot
      """,
      graphInterfaceSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(robotGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(returnType).isEqualTo(Robot::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(RobotKey::class.java).value.java).isEqualTo(testRobot)
      }

      assertThat(graphInterface.newMetroGraph<TestRobotGraph>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a graph interface is generated with @Inject constructor`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject

      @Inject
      @ContributesRobot(AppScope::class)
      class TestRobot : Robot
      """,
      graphInterfaceSource,
    ) {
      val robotGraph = testRobot.graph

      assertThat(robotGraph.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      assertThat(
          robotGraph.declaredNonSyntheticMethods.singleOrNull { it.name == "provideTestRobot" }
        )
        .isNull()

      with(robotGraph.declaredNonSyntheticMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(returnType).isEqualTo(Robot::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(RobotKey::class.java).value.java).isEqualTo(testRobot)
      }

      assertThat(graphInterface.newMetroGraph<TestRobotGraph>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a graph interface is generated without direct super type`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      interface BaseRobot1 : Robot
      abstract class BaseRobot2 : BaseRobot1

      @ContributesRobot(AppScope::class)
      class TestRobot : BaseRobot2()
      """
    ) {
      assertThat(testRobot.graph).isNotNull()
    }
  }

  @Test
  fun `the robot class must be a super type`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope

      interface BaseRobot1
      abstract class BaseRobot2 : BaseRobot1

      @ContributesRobot(AppScope::class)
      class TestRobot : BaseRobot2()
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "In order to use @ContributesRobot, TestRobot must implement " +
            "software.amazon.app.platform.robot.Robot."
        )
    }
  }

  @Test
  fun `a Robot must not be a singleton`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import dev.zacsweers.metro.AppScope
      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn

      @Inject
      @SingleIn(AppScope::class)
      @ContributesRobot(AppScope::class)
      class TestRobot : Robot
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "It's not allowed allowed for a robot to be a singleton, because " +
            "the lifetime of the robot is scoped to the robot() factory function. " +
            "Remove the @SingleIn annotation."
        )
    }
  }

  @Test
  fun `only the app scope is supported for now`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.robot.ContributesRobot
      import software.amazon.app.platform.robot.Robot
      import software.amazon.lastmile.kotlin.inject.anvil.AppScope

      @ContributesRobot(String::class)
      class TestRobot : Robot
              """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "Robots can only be contributed to the AppScope for now. " +
            "Scope kotlin.String is unsupported."
        )
    }
  }

  @Language("kotlin")
  private val graphInterfaceSource =
    """
        package software.amazon.test

        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.createGraph
        import dev.zacsweers.metro.DependencyGraph
        import dev.zacsweers.metro.SingleIn

        @DependencyGraph(AppScope::class)
        @SingleIn(AppScope::class)
        interface GraphInterface : TestRobotGraph {
            companion object {
                fun create(): GraphInterface = createGraph<GraphInterface>()
            }
        }
    """

  private val JvmCompilationResult.testRobot: Class<*>
    get() = classLoader.loadClass("software.amazon.test.TestRobot")

  private val Class<*>.graph: Class<*>
    get() =
      classLoader.loadClass(
        "$METRO_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "Graph"
      )
}
