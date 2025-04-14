@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject.processor

import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import com.tschuchort.compiletesting.JvmCompilationResult
import kotlin.test.assertFailsWith
import me.tatarka.inject.annotations.IntoSet
import me.tatarka.inject.annotations.Provides
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.amazon.app.platform.inject.capitalize
import software.amazon.app.platform.inject.compile
import software.amazon.app.platform.inject.componentInterface
import software.amazon.app.platform.inject.generatedComponent
import software.amazon.app.platform.inject.inner
import software.amazon.app.platform.inject.isAnnotatedWith
import software.amazon.app.platform.inject.newComponent
import software.amazon.app.platform.inject.origin
import software.amazon.app.platform.scope.Scoped
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope

class ContributesBindingScopedProcessorTest {

  @Test
  fun `a binding method for Scoped is generated`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base, Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent

      assertThat(generatedComponent.origin).isEqualTo(impl)
      assertThat(generatedComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(AppScope::class)

      with(generatedComponent.declaredMethods.single { it.name == "provideImplScoped" }) {
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated for inner classes`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            interface Impl {
                @Inject
                @ContributesBinding(Unit::class)
                class Inner : Base, Scoped
            } 
            """
    ) {
      val generatedComponent = impl.inner.scopedComponent

      assertThat(generatedComponent.origin).isEqualTo(impl.inner)
      assertThat(generatedComponent.getAnnotation(ContributesTo::class.java).scope)
        .isEqualTo(Unit::class)

      with(generatedComponent.declaredMethods.single { it.name == "provideImplInnerScoped" }) {
        assertThat(parameters.single().type).isEqualTo(impl.inner)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(Unit::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated for repeated annotations`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base
            interface Base2

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Base2::class)
            class Impl : Base, Base2, Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent

      with(generatedComponent.declaredMethods.single { it.name == "provideImplScoped" }) {
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `a binding method for Scoped is generated without any other binding`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Scoped
            """
    ) {
      val generatedComponent = impl.scopedComponent
      with(generatedComponent.declaredMethods.single()) {
        assertThat(name).isEqualTo("provideImplScoped")
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }

      // Because Scoped is the only super type.
      assertFailsWith<ClassNotFoundException> { impl.generatedComponent }
    }
  }

  @Test
  fun `a binding method for Scoped is generated only explicitly when Scoped is part of the supertype hierarchy`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base : Scoped

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class, boundType = Base::class)
            @ContributesBinding(AppScope::class, boundType = Scoped::class)
            class Impl2 : Base
            """
    ) {
      with(impl.generatedComponent.declaredMethods.single()) {
        assertThat(name).isEqualTo("provideImplBase")
        assertThat(parameters.single().type).isEqualTo(impl)
        assertThat(returnType).isEqualTo(base)
        assertThat(this).isAnnotatedWith(Provides::class)
      }
      // Because Scoped is not a direct super type.
      assertFailsWith<ClassNotFoundException> { impl.scopedComponent }

      with(impl2.generatedComponent.declaredMethods.single()) {
        assertThat(parameters.single().type).isEqualTo(impl2)
        assertThat(returnType).isEqualTo(base)
        assertThat(this).isAnnotatedWith(Provides::class)
      }
      with(impl2.scopedComponent.declaredMethods.single { it.name == "provideImpl2Scoped" }) {
        assertThat(parameters.single().type).isEqualTo(impl2)
        assertThat(returnType).isEqualTo(scoped)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoSet::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(AppScope::class)
      }
    }
  }

  @Test
  fun `scoped instances are added to the component`() {
    compile(
      """
            package software.amazon.test
    
            import software.amazon.app.platform.renderer.RendererComponent
            import software.amazon.app.platform.robot.RobotComponent
            import software.amazon.app.platform.scope.Scoped
            import me.tatarka.inject.annotations.Inject
            import me.tatarka.inject.annotations.Component
            import software.amazon.lastmile.kotlin.inject.anvil.AppScope
            import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
            import software.amazon.lastmile.kotlin.inject.anvil.ForScope
            import software.amazon.lastmile.kotlin.inject.anvil.MergeComponent
            import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

            interface Base

            @Inject
            @SingleIn(AppScope::class)
            @ContributesBinding(AppScope::class)
            class Impl : Base, Scoped

            @Inject
            @SingleIn(Unit::class)
            @ContributesBinding(Unit::class)
            class Impl2 : Base, Scoped

            @Component
            @MergeComponent(AppScope::class, exclude = [RendererComponent::class, RobotComponent::class])
            @SingleIn(AppScope::class)
            interface ComponentInterface : ComponentInterfaceMerged {
                @ForScope(AppScope::class)
                val scoped: Set<Scoped>
            }

            @Component
            @MergeComponent(Unit::class)
            @SingleIn(Unit::class)
            interface ComponentInterface2 : ComponentInterface2Merged {
                @ForScope(Unit::class)
                val scoped: Set<Scoped>
            }
            """
    ) {
      val component = componentInterface.newComponent<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped =
        component::class.java.declaredMethods.single { it.name == "getScoped" }.invoke(component)
          as Set<Scoped>

      assertThat(scoped).hasSize(1)
      assertThat(scoped.single()::class.java).isEqualTo(impl)

      val component2 = componentInterface2.newComponent<Any>()

      @Suppress("UNCHECKED_CAST")
      val scoped2 =
        component2::class.java.declaredMethods.single { it.name == "getScoped" }.invoke(component2)
          as Set<Scoped>

      assertThat(scoped2).hasSize(1)
      assertThat(scoped2.single()::class.java).isEqualTo(impl2)
    }
  }

  private val Class<*>.scopedComponent: Class<*>
    get() =
      classLoader.loadClass(
        "$APP_PLATFORM_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter("$packageName.").split(".").joinToString(separator = "") {
            it.capitalize()
          } +
          "ScopedComponent"
      )

  private val JvmCompilationResult.componentInterface2: Class<*>
    get() = classLoader.loadClass("software.amazon.test.ComponentInterface2")

  private val JvmCompilationResult.base: Class<*>
    get() = classLoader.loadClass("software.amazon.test.Base")

  private val JvmCompilationResult.impl: Class<*>
    get() = classLoader.loadClass("software.amazon.test.Impl")

  private val JvmCompilationResult.impl2: Class<*>
    get() = classLoader.loadClass("software.amazon.test.Impl2")

  private val scoped: Class<*>
    get() = Scoped::class.java
}
