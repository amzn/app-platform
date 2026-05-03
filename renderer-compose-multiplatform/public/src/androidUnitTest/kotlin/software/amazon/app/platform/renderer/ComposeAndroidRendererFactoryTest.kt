package software.amazon.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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

class ComposeAndroidRendererFactoryTest {

  @Test
  fun `a ComposeRenderer is wrapped for Android View support`() = runTest {
    val factory = factory()
    assertThat(factory.getRenderer(ComposeModel::class))
      .isInstanceOf<ComposeWithinAndroidViewRenderer<ComposeModel>>()
  }

  @Test
  fun `a ViewRenderer is wrapped for Compose support`() = runTest {
    val factory = factory()
    assertThat(factory.getRenderer(AndroidModel::class))
      .isInstanceOf<AndroidViewWithinComposeRenderer<AndroidModel>>()
  }

  @Test
  fun `an unsupported renderer cannot be wrapped`() = runTest {
    val factory = factory()
    assertFailure { factory.getRenderer(UnsupportedModel::class) }
      .isInstanceOf<IllegalStateException>()
      .messageContains(
        "Unsupported renderer type class software.amazon.app.platform." +
          "renderer.ComposeAndroidRendererFactoryTest\$UnsupportedRenderer"
      )
  }

  private fun TestScope.factory(): ComposeAndroidRendererFactory {
    return ComposeAndroidRendererFactory.createForComposeUi(rootScopeProvider())
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
                    AndroidModel::class to provider { TestAndroidRenderer() },
                    UnsupportedModel::class to provider { UnsupportedRenderer() },
                  )
                override val modelToRendererMapping:
                  Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                  mapOf(
                    ComposeModel::class to TestComposeRenderer::class,
                    AndroidModel::class to TestAndroidRenderer::class,
                    UnsupportedModel::class to UnsupportedRenderer::class,
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

  private class UnsupportedModel : BaseModel

  private class TestComposeRenderer : ComposeRenderer<ComposeModel>() {
    @Composable override fun Compose(model: ComposeModel, modifier: Modifier) = Unit
  }

  private class TestAndroidRenderer : ViewRenderer<AndroidModel>() {
    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: AndroidModel,
    ): View = throw NotImplementedError()
  }

  private class UnsupportedRenderer : Renderer<UnsupportedModel> {
    override fun render(model: UnsupportedModel) = throw NotImplementedError()
  }
}
