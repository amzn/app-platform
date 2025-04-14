@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.amazon.app.platform.inject.capitalize
import software.amazon.app.platform.inject.compile
import software.amazon.app.platform.inject.componentInterface
import software.amazon.app.platform.inject.isAnnotatedWith
import software.amazon.app.platform.inject.newComponent
import software.amazon.app.platform.inject.origin
import software.amazon.app.platform.robot.RobotComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

class ContributesRobotGeneratorTest {

  @Test
  fun `a component interface is generated without @Inject constructor`() {
    compile(
      """
            package software.amazon.test

            import software.amazon.app.platform.inject.robot.ContributesRobot
            import software.amazon.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @ContributesRobot(AppScope::class)
            class TestRobot : Robot
            """,
      componentInterfaceSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(robotComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)
      assertThat(robotComponent.origin).isEqualTo(testRobot)

      with(robotComponent.declaredMethods.single { it.name == "provideTestRobot" }) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRobot)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(robotComponent.declaredMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type.canonicalName)
          .isEqualTo("kotlin.jvm.functions.Function0")
        assertThat(returnType).isEqualTo(Pair::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
      }

      assertThat(componentInterface.newComponent<RobotComponent>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a component interface is generated with @Inject constructor`() {
    compile(
      """
            package software.amazon.test

            import software.amazon.app.platform.inject.robot.ContributesRobot
            import software.amazon.app.platform.robot.Robot
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            @Inject
            @ContributesRobot(AppScope::class)
            class TestRobot : Robot
            """,
      componentInterfaceSource,
    ) {
      val robotComponent = testRobot.component

      assertThat(robotComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)
      assertThat(robotComponent.origin).isEqualTo(testRobot)

      assertThat(robotComponent.declaredMethods.singleOrNull { it.name == "provideTestRobot" })
        .isNull()

      with(robotComponent.declaredMethods.single { it.name == "provideTestRobotIntoMap" }) {
        assertThat(parameters.single().type.canonicalName)
          .isEqualTo("kotlin.jvm.functions.Function0")
        assertThat(returnType).isEqualTo(Pair::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
      }

      assertThat(componentInterface.newComponent<RobotComponent>().robots.keys)
        .containsOnly(testRobot.kotlin)
    }
  }

  @Test
  fun `a component interface is generated without direct super type`() {
    compile(
      """
            package software.amazon.test

            import software.amazon.app.platform.inject.robot.ContributesRobot
            import software.amazon.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

            interface BaseRobot1 : Robot
            abstract class BaseRobot2 : BaseRobot1

            @ContributesRobot(AppScope::class)
            class TestRobot : BaseRobot2()
            """
    ) {
      assertThat(testRobot.component).isNotNull()
    }
  }

  @Test
  fun `the robot class must be a super type`() {
    compile(
      """
            package software.amazon.test

            import software.amazon.app.platform.inject.robot.ContributesRobot
            import software.amazon.app.platform.robot.Robot
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope

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
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

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
  private val componentInterfaceSource =
    """
        package software.amazon.test

        import software.amazon.app.platform.renderer.RendererComponent
        import me.tatarka.inject.annotations.Component
        import software.amazon.lastmile.kotlin.inject.anvil.AppScope
        import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
        import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

        @Component
        @MergeComponent(AppScope::class, exclude = [RendererComponent::class])
        @SingleIn(AppScope::class)
        interface ComponentInterface : ComponentInterfaceMerged
    """

  private val JvmCompilationResult.testRobot: Class<*>
    get() = classLoader.loadClass("software.amazon.test.TestRobot")

  private val Class<*>.component: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).split(".").joinToString(
            separator = ""
          ) {
            it.capitalize()
          } +
          "Component"
      )
}
