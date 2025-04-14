package software.amazon.app.platform.renderer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.amazon.app.platform.scope.di.addDiComponent

class TestApplication : Application(), RootScopeProvider {

  var rendererComponent: RendererComponent? = null

  override val rootScope: Scope =
    Scope.buildRootScope {
      addDiComponent(Component())
      addCoroutineScopeScoped(CoroutineScopeScoped(Job() + CoroutineName("test")))
    }

  private inner class Component : ViewRenderer.Component, RendererComponent.Parent {
    override val dispatcher: CoroutineDispatcher = Dispatchers.Main.immediate

    override fun rendererComponent(factory: RendererFactory): RendererComponent =
      requireNotNull(rendererComponent)
  }
}

val testApplication: TestApplication
  get() =
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApplication
