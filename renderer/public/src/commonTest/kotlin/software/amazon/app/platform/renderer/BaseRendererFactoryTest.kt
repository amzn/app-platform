package software.amazon.app.platform.renderer

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isSameInstanceAs
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertFailsWith
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.addDiComponent

class BaseRendererFactoryTest {

  @Test
  fun `creating a renderer without mapping throws an error`() {
    val exception =
      assertFailsWith<Exception> {
        rendererFactory(renderers = emptyMap(), modelToRendererMapping = emptyMap())
          .getRenderer(TestModel(1))
      }

    // Transform the message, because it's slightly different on iOS than on
    // Android and Desktop.
    val message =
      exception.message?.replace('\$', '.')?.replace(" (Kotlin reflection is not available)", "")

    assertThat(message)
      .isEqualTo(
        "No renderer was provided for class " +
          "software.amazon.app.platform.renderer.BaseRendererFactoryTest.TestModel. " +
          "Did you add @ContributesRenderer?"
      )
  }

  @Test
  fun `the createRenderer function always creates new renderers`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.createRenderer(model)).isNotSameInstanceAs(factory.createRenderer(model))
  }

  @Test
  fun `the createRenderer function always creates new renderers after a renderer was cached`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.getRenderer(model)).isNotSameInstanceAs(factory.createRenderer(model))
  }

  @Test
  fun `the getRenderer function caches renderers based on type`() {
    val factory = rendererFactory()
    val model1 = TestModel(1)
    val model2 = TestModel(2)

    assertThat(factory.getRenderer(model1)).isSameInstanceAs(factory.getRenderer(model2))
  }

  @Test
  fun `the getRenderer function caches renderers based on type and id`() {
    val factory = rendererFactory()
    val model1 = TestModel(1)
    val model2 = TestModel(2)

    assertThat(factory.getRenderer(model1, rendererId = 1))
      .isSameInstanceAs(factory.getRenderer(model2, rendererId = 1))
  }

  @Test
  fun `the getRenderer function returns a new renderer on ID mismatch`() {
    val factory = rendererFactory()
    val model = TestModel(1)

    assertThat(factory.getRenderer(model, rendererId = 1))
      .isNotSameInstanceAs(factory.getRenderer(model, rendererId = 2))
  }

  @Test
  fun `the getRenderer function caches renderers based on the sealed type`() {
    val factory = rendererFactory()
    val model1 = SealedTestModel.Model1(1)
    val model2 = SealedTestModel.Model2(1)

    assertThat(factory.getRenderer(model1)).isSameInstanceAs(factory.getRenderer(model2))
  }

  private fun rendererFactory(
    renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
      mapOf(
        TestModel::class to { TestRenderer() },
        SealedTestModel::class to { SealedTestRenderer() },
        SealedTestModel.Model1::class to { SealedTestRenderer() },
        SealedTestModel.Model2::class to { SealedTestRenderer() },
      ),
    modelToRendererMapping: Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
      mapOf(
        TestModel::class to TestRenderer::class,
        SealedTestModel::class to SealedTestRenderer::class,
        SealedTestModel.Model1::class to SealedTestRenderer::class,
        SealedTestModel.Model2::class to SealedTestRenderer::class,
      ),
  ): RendererFactory =
    BaseRendererFactory(
      rootScopeProvider =
        object : RootScopeProvider {
          override val rootScope: Scope =
            Scope.buildRootScope {
              addDiComponent(
                object : RendererComponent.Parent {
                  override fun rendererComponent(factory: RendererFactory): RendererComponent =
                    object : RendererComponent {
                      override val renderers: Map<KClass<out BaseModel>, () -> Renderer<*>> =
                        renderers

                      override val modelToRendererMapping:
                        Map<KClass<out BaseModel>, KClass<out Renderer<*>>> =
                        modelToRendererMapping
                    }
                }
              )
            }
        }
    )

  private data class TestModel(val value: Int) : BaseModel

  private class TestRenderer : Renderer<TestModel> {
    override fun render(model: TestModel) = Unit
  }

  private sealed interface SealedTestModel : BaseModel {
    data class Model1(val value: Int) : SealedTestModel

    data class Model2(val value: Int) : SealedTestModel
  }

  private class SealedTestRenderer : Renderer<SealedTestModel> {
    override fun render(model: SealedTestModel) = Unit
  }
}
