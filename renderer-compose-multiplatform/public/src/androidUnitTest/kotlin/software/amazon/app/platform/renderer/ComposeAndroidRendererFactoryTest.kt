package software.amazon.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.Composable
import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.buildTestScope
import software.amazon.app.platform.scope.di.addDiComponent

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
    val activity = Activity()
    val viewGroup = FrameLayout(activity)

    return ComposeAndroidRendererFactory(rootScopeProvider(), activity, viewGroup)
  }

  private fun TestScope.rootScopeProvider(): RootScopeProvider {
    val scope =
      Scope.buildTestScope(this) {
        addDiComponent(
          object : RendererComponent.Parent {
            override fun rendererComponent(factory: RendererFactory): RendererComponent {
              return object : RendererComponent {
                override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
                  mapOf(
                    ComposeModel::class to { TestComposeRenderer() },
                    AndroidModel::class to { TestAndroidRenderer() },
                    UnsupportedModel::class to { UnsupportedRenderer() },
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
    @Composable override fun Compose(model: ComposeModel) = Unit
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
