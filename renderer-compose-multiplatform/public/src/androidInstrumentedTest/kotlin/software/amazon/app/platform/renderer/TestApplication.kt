package software.amazon.app.platform.renderer

import android.app.Application
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.Job
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.coroutine.CoroutineScopeScoped
import software.amazon.app.platform.scope.coroutine.addCoroutineScopeScoped
import software.amazon.app.platform.scope.di.metro.addMetroDependencyGraph

class TestApplication : Application(), RootScopeProvider {

  var rendererGraph: RendererGraph? = null

  override val rootScope: Scope = Scope.buildRootScope {
    addMetroDependencyGraph(Graph())
    addCoroutineScopeScoped(CoroutineScopeScoped(Job() + CoroutineName("test")))
  }

  private inner class Graph : RendererGraph.Factory {
    override fun createRendererGraph(factory: RendererFactory): RendererGraph =
      requireNotNull(rendererGraph)
  }
}

val testApplication: TestApplication
  get() =
    InstrumentationRegistry.getInstrumentation().targetContext.applicationContext as TestApplication
