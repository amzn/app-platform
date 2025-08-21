package software.amazon.app.platform.scope.di.metro

import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isSameInstanceAs
import software.amazon.app.platform.internal.IgnoreWasm
import software.amazon.app.platform.internal.Platform
import software.amazon.app.platform.internal.platform
import software.amazon.app.platform.scope.Scope
import kotlin.test.Test
import kotlin.test.assertFailsWith

class MetroServiceTest {

  @Test
  fun `a metro component can be registered in a scope`() {
    val component = ParentComponentImpl()

    val scope = Scope.buildRootScope { addMetroComponent(component) }

    assertThat(scope.metroComponent<ParentComponent>()).isSameInstanceAs(component)
  }

  @Test
  @IgnoreWasm
  fun `if a metro component cannot be found then an exception is thrown with a helpful error message`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addMetroComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addMetroComponent(childComponent) }

    val exception = assertFailsWith<NoSuchElementException> { childScope.metroComponent<Unit>() }

    val kotlinReflectWarning =
      when (platform) {
        Platform.JVM -> " (Kotlin reflection is not available)"
        Platform.Native,
        Platform.Web -> ""
      }

    assertThat(exception)
      .hasMessage(
        "Couldn't find component implementing class kotlin.Unit$kotlinReflectWarning. " +
          "Inspected: [ChildComponentImpl, ParentComponentImpl] (fully qualified names: " +
          "[class software.amazon.app.platform.scope.di.metro.MetroServiceTest." +
          "ChildComponentImpl$kotlinReflectWarning, class software.amazon.app." +
          "platform.scope.di.metro.MetroServiceTest.ParentComponentImpl" +
          "$kotlinReflectWarning])"
      )
  }

  @Test
  fun `a DI component can be retrieved from a scope`() {
    val parentComponent = ParentComponentImpl()
    val childComponent = ChildComponentImpl()

    val parentScope = Scope.buildRootScope { addMetroComponent(parentComponent) }
    val childScope = parentScope.buildChild("child") { addMetroComponent(childComponent) }

    assertThat(childScope.metroComponent<ChildComponent>()).isSameInstanceAs(childComponent)
    assertThat(childScope.metroComponent<ParentComponent>()).isSameInstanceAs(parentComponent)

    assertThat(parentScope.metroComponent<ParentComponent>()).isSameInstanceAs(parentComponent)
    assertFailsWith<NoSuchElementException> { parentScope.metroComponent<ChildComponent>() }
  }

  private interface ParentComponent

  private class ParentComponentImpl : ParentComponent

  private interface ChildComponent

  private class ChildComponentImpl : ChildComponent
}
