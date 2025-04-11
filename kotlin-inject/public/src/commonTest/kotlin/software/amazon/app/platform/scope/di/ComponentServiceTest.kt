package software.amazon.app.platform.scope.di

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isSameInstanceAs
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.amazon.app.platform.scope.Scope

class ComponentServiceTest {

  @Test
  fun `a DI component can be registered in a scope`() {
    val component = ParentComponentImpl()

    val scope = Scope.buildRootScope { addDiComponent(component) }

    assertThat(scope.diComponent<ParentComponent>()).isSameInstanceAs(component)
  }

  @Test
  fun `if a DI component cannot be found then an exception is thrown with a helpful error message`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addDiComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addDiComponent(childComponent) }

    val exception = assertFailsWith<NoSuchElementException> { childScope.diComponent<Unit>() }

    val kotlinReflectWarning =
      when (platform) {
        Platform.JVM -> " (Kotlin reflection is not available)"
        Platform.Native -> ""
      }

    assertThat(exception)
      .hasMessage(
        "Couldn't find component implementing class kotlin.Unit$kotlinReflectWarning. " +
          "Inspected: [ChildComponentImpl, ParentComponentImpl] (fully qualified names: " +
          "[class software.amazon.app.platform.scope.di.ComponentServiceTest." +
          "ChildComponentImpl$kotlinReflectWarning, class software.amazon.app." +
          "platform.scope.di.ComponentServiceTest.ParentComponentImpl" +
          "$kotlinReflectWarning])"
      )
  }

  @Test
  fun `a DI component can be retrieved from a scope`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addDiComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addDiComponent(childComponent) }

    assertThat(childScope.diComponent<ChildComponent>()).isSameInstanceAs(childComponent)
    assertThat(childScope.diComponent<ParentComponent>()).isSameInstanceAs(parentComponent)

    assertThat(parentScope.diComponent<ParentComponent>()).isSameInstanceAs(parentComponent)
    assertFailsWith<NoSuchElementException> { parentScope.diComponent<ChildComponent>() }
  }

  private interface ParentComponent

  private class ParentComponentImpl : ParentComponent

  private interface ChildComponent

  private class ChildComponentImpl : ChildComponent
}
