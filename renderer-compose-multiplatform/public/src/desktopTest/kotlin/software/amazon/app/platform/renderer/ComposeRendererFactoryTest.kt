package software.amazon.app.platform.renderer

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import assertk.all
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.provider
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.buildTestScope
import software.amazon.app.platform.scope.di.metro.addMetroDependencyGraph

class ComposeRendererFactoryTest {

  @Test
  fun `a ComposeRendererFactory can create a ComposeRenderer`() = runTest {
    val factory = ComposeRendererFactory(rootScopeProvider())
    assertThat(factory.getRenderer(ComposeModel::class)).isInstanceOf<TestComposeRenderer>()
  }

  @Test
  fun `a ComposeRendererFactory throws an error for other renderer types`() = runTest {
    val factory = ComposeRendererFactory(rootScopeProvider())

    assertFailure { factory.getRenderer(AndroidModel::class) }
      .isInstanceOf<IllegalStateException>()
      .messageContains("Expected a ComposeRenderer for model type")
  }

  @Test
  fun `getComposeRenderer() can create a ComposeRenderer`() = runTest {
    val factory = ComposeRendererFactory(rootScopeProvider())
    assertThat(factory.getComposeRenderer(ComposeModel::class)).isInstanceOf<TestComposeRenderer>()
  }

  @Test
  fun `getComposeRenderer() throws an error for other renderer types`() = runTest {
    val factory = BaseRendererFactory(rootScopeProvider())

    assertFailure { factory.getComposeRenderer(AndroidModel::class) }
      .all {
        isInstanceOf<IllegalStateException>()

        messageContains(
          "The renderer class software.amazon.app.platform.renderer." +
            "ComposeRendererFactoryTest\$AndroidRenderer"
        )
        messageContains(
          "For Android View and Compose UI interop use ComposeAndroidRendererFactory."
        )
      }
  }

  private fun TestScope.rootScopeProvider(): RootScopeProvider {
    val scope =
      Scope.buildTestScope(this) {
        addMetroDependencyGraph(
          object : RendererGraph.Factory {
            override fun createRendererGraph(factory: RendererFactory): RendererGraph {
              return object : RendererGraph {
                override val renderers: Map<KClass<out BaseModel>, Provider<Renderer<*>>> =
                  mapOf(
                    ComposeModel::class to provider { TestComposeRenderer() },
                    AndroidModel::class to provider { AndroidRenderer() },
                  )
                override val modelToRendererMapping:
                  Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                  mapOf(
                    ComposeModel::class to TestComposeRenderer::class,
                    AndroidModel::class to AndroidRenderer::class,
                  )
              }
            }
          }
        )
      }
    return object : RootScopeProvider {
      override val rootScope: Scope = scope
    }
  }

  private class ComposeModel : BaseModel

  private class AndroidModel : BaseModel

  private class TestComposeRenderer : ComposeRenderer<ComposeModel>() {
    @Composable override fun Compose(model: ComposeModel, modifier: Modifier) = Unit
  }

  private class AndroidRenderer : Renderer<AndroidModel> {
    override fun render(model: AndroidModel) = Unit
  }
}
