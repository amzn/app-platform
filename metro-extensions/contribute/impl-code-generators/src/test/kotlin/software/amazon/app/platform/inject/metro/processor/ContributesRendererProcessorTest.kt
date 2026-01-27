@file:OptIn(ExperimentalCompilerApi::class)

package software.amazon.app.platform.inject.metro.processor

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.containsExactlyInAnyOrder
import assertk.assertions.containsOnly
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.startsWith
import com.tschuchort.compiletesting.JvmCompilationResult
import com.tschuchort.compiletesting.KotlinCompilation.ExitCode.COMPILATION_ERROR
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi
import org.junit.jupiter.api.Test
import software.amazon.app.platform.inject.metro.compile
import software.amazon.app.platform.inject.metro.declaredNonSyntheticMethods
import software.amazon.app.platform.inject.metro.graphInterface
import software.amazon.app.platform.inject.metro.newMetroGraph
import software.amazon.app.platform.ksp.inner
import software.amazon.app.platform.ksp.isAnnotatedWith
import software.amazon.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererScope
import software.amazon.app.platform.renderer.metro.RendererKey
import software.amazon.test.TestRendererGraph

class ContributesRendererProcessorTest {

  @Test
  fun `a graph interface is generated in the lookup package for a contributed renderer`() {
    compile(
      """
      package software.amazon.test
    
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.inject.ContributesRenderer

      class Model : BaseModel

      @ContributesRenderer
      class TestRenderer : Renderer<Model> {
          override fun render(model: Model) = Unit
      }
      """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.rendererGraph

      assertThat(generatedGraph.packageName).startsWith(METRO_LOOKUP_PACKAGE)

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRenderer"
        }
      ) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRenderer)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRendererModel"
        }
      ) {
        assertThat(parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(returnType).isEqualTo(Renderer::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(this.getAnnotation(RendererKey::class.java).value).isEqualTo(model)
      }

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRendererModelKey"
        }
      ) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(KClass::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(ForScope::class.java).scope).isEqualTo(RendererScope::class)
        assertThat(getAnnotation(RendererKey::class.java).value).isEqualTo(model)
      }

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(model)

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.keys)
        .containsOnly(model)

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.values)
        .containsOnly(testRenderer.kotlin)
    }
  }

  @Test
  fun `a graph interface is generated in the lookup package for a contributed renderer as inner class`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.inject.ContributesRenderer

      class Model : BaseModel

      class TestRenderer {
          @ContributesRenderer
          class Inner : Renderer<Model> {
              override fun render(model: Model) = Unit
          }
      }
      """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.inner.rendererGraph

      assertThat(generatedGraph.packageName).startsWith(METRO_LOOKUP_PACKAGE)

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRendererInner"
        }
      ) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRenderer.inner)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRendererInnerModel"
        }
      ) {
        assertThat(parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(returnType).isEqualTo(Renderer::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
        assertThat(getAnnotation(RendererKey::class.java).value).isEqualTo(model)
      }

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(model)
    }
  }

  @Test
  fun `a graph interface is generated in the lookup package for a contributed renderer with a model as inner class`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.inject.ContributesRenderer

      class Presenter {
          class Model : BaseModel
      }

      @ContributesRenderer
      class TestRenderer : Renderer<Presenter.Model> {
          override fun render(model: Presenter.Model) = Unit
      }
            """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.rendererGraph

      assertThat(generatedGraph.packageName).startsWith(METRO_LOOKUP_PACKAGE)

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRenderer"
        }
      ) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRenderer)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRendererPresenterModel"
        }
      ) {
        assertThat(parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(returnType).isEqualTo(Renderer::class.java)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(this).isAnnotatedWith(IntoMap::class)
      }

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(presenter.model.kotlin)
    }
  }

  @Test
  fun `the explicit model type has a higher priority`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model : BaseModel
      class Model2 : BaseModel

      @ContributesRenderer(Model::class)
      class TestRenderer : Renderer<Model2> {
        override fun render(model: Model2) = Unit
      }
      """,
      graphInterfaceSource,
    ) {
      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(model)
    }
  }

  @Test
  fun `the model type can be inferred from the class hierarchy`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model : BaseModel

      interface OtherRenderer : Renderer<Model>

      @ContributesRenderer
      class TestRenderer : OtherRenderer {
        override fun render(model: Model) = Unit
      }
      """
    ) {
      assertThat(testRenderer.modelType).isEqualTo(model)
    }
  }

  @Test
  fun `the model type can be inferred from the class hierarchy with multiple levels`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model : BaseModel

      interface OtherRenderer<S : Any, T : BaseModel, U : CharSequence> : Renderer<T>
      interface OtherRenderer2<S : BaseModel, T : Any> : OtherRenderer<T, S, String>
      interface OtherRenderer3 : OtherRenderer2<Model, Any>
      interface OtherRenderer4 : OtherRenderer3


      @ContributesRenderer
      class TestRenderer : OtherRenderer4 {
        override fun render(model: Model) = Unit
      }
      """
    ) {
      assertThat(testRenderer.modelType).isEqualTo(model)
    }
  }

  @Test
  fun `the model type must be explicit when it cannot be inferred`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model1 : BaseModel
      class Model2 : BaseModel

      interface OtherRenderer<S : BaseModel, T : BaseModel> : Renderer<S>

      @ContributesRenderer
      class TestRenderer : OtherRenderer<Model1, Model2> {
        override fun render(model: Model) = Unit
      }
      """,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "Couldn't find BaseModel type for TestRenderer. Consider adding " +
            "an explicit parameter.Found: software.amazon.test.Model1, software.amazon.test.Model2"
        )
    }
  }

  @Test
  fun `the graph interface contains multiple binding methods for model hierarchies`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.inject.ContributesRenderer

      interface Presenter {
        sealed interface Model : BaseModel {
          sealed interface Inner : Model {
            data object Model1 : Inner
            data object Model2 : Inner
          }
          data object Model2 : Model

          // Note that this class doesn't extend Model.
          class OtherSubclass
        }
      }

      @ContributesRenderer
      class TestRenderer : Renderer<Presenter.Model> {
        override fun render(model: Presenter.Model) = Unit
      }
      """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.rendererGraph

      with(
        generatedGraph.declaredNonSyntheticMethods.single {
          it.name == "provideSoftwareAmazonTestTestRenderer"
        }
      ) {
        assertThat(parameters).isEmpty()
        assertThat(returnType).isEqualTo(testRenderer)
        assertThat(this).isAnnotatedWith(Provides::class)
        assertThat(getAnnotation(SingleIn::class.java)).isNull()
      }

      val bindingMethods =
        generatedGraph.declaredNonSyntheticMethods.filter {
          it.name.startsWith("provideSoftwareAmazonTestTestRendererPresenterModel") &&
            !it.name.endsWith("Key")
        }
      assertThat(bindingMethods.map { it.name })
        .containsExactlyInAnyOrder(
          "provideSoftwareAmazonTestTestRendererPresenterModel",
          "provideSoftwareAmazonTestTestRendererPresenterModelInner",
          "provideSoftwareAmazonTestTestRendererPresenterModelInnerModel1",
          "provideSoftwareAmazonTestTestRendererPresenterModelInnerModel2",
          "provideSoftwareAmazonTestTestRendererPresenterModelModel2",
        )

      bindingMethods.forEach {
        assertThat(it.parameters.single().type).isEqualTo(Provider::class.java)
        assertThat(it.returnType).isEqualTo(Renderer::class.java)
        assertThat(it).isAnnotatedWith(Provides::class)
        assertThat(it).isAnnotatedWith(IntoMap::class)
        assertThat(it).isAnnotatedWith(RendererKey::class)
      }

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsExactlyInAnyOrder(
          presenter.model.kotlin,
          presenter.model.inner.kotlin,
          presenter.model.inner.model1.kotlin,
          presenter.model.inner.model2.kotlin,
          presenter.model.model2.kotlin,
        )

      val keyBindingMethods =
        generatedGraph.declaredNonSyntheticMethods.filter {
          it.name.startsWith("provideSoftwareAmazonTestTestRendererPresenterModel") &&
            it.name.endsWith("Key")
        }
      assertThat(keyBindingMethods.map { it.name })
        .containsExactlyInAnyOrder(
          "provideSoftwareAmazonTestTestRendererPresenterModelKey",
          "provideSoftwareAmazonTestTestRendererPresenterModelInnerKey",
          "provideSoftwareAmazonTestTestRendererPresenterModelInnerModel1Key",
          "provideSoftwareAmazonTestTestRendererPresenterModelInnerModel2Key",
          "provideSoftwareAmazonTestTestRendererPresenterModelModel2Key",
        )

      keyBindingMethods.forEach {
        assertThat(it.parameters).isEmpty()
        assertThat(it.returnType).isEqualTo(KClass::class.java)
        assertThat(it).isAnnotatedWith(Provides::class)
        assertThat(it).isAnnotatedWith(IntoMap::class)
        assertThat(it).isAnnotatedWith(ForScope::class)
      }

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.keys)
        .containsExactlyInAnyOrder(
          presenter.model.kotlin,
          presenter.model.inner.kotlin,
          presenter.model.inner.model1.kotlin,
          presenter.model.inner.model2.kotlin,
          presenter.model.model2.kotlin,
        )

      assertThat(
          graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.values.distinct()
        )
        .containsOnly(testRenderer.kotlin)
    }
  }

  @Test
  fun `the binding methods for subtypes are not generated when disabled`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.inject.ContributesRenderer

      interface Presenter {
        sealed interface Model : BaseModel {
          data object Model1 : Model
          data object Model2 : Model
        }
      }

      @ContributesRenderer(includeSealedSubtypes = false)
      class TestRenderer : Renderer<Presenter.Model> {
        override fun render(model: Presenter.Model) = Unit
      }
      """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.rendererGraph

      assertThat(
          generatedGraph.declaredNonSyntheticMethods
            .filter {
              it.name.startsWith("provideSoftwareAmazonTestTestRendererPresenterModel") &&
                !it.name.endsWith("Key")
            }
            .map { it.name }
        )
        .containsOnly("provideSoftwareAmazonTestTestRendererPresenterModel")

      assertThat(
          generatedGraph.declaredNonSyntheticMethods
            .filter { it.name.startsWith("provideSoftwareAmazonTestTestRendererPresenterModelKey") }
            .map { it.name }
        )
        .containsOnly("provideSoftwareAmazonTestTestRendererPresenterModelKey")

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(presenter.model.kotlin)

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.keys)
        .containsOnly(presenter.model.kotlin)

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().modelToRendererMapping.values)
        .containsOnly(testRenderer.kotlin)
    }
  }

  @Test
  fun `the graph does not contain a binding for the renderer if it is annotated with @Inject`() {
    compile(
      """
      package software.amazon.test

      import dev.zacsweers.metro.Inject
      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model : BaseModel

      @ContributesRenderer
      @Inject
      class TestRenderer(@Suppress("unused") val string: String) : Renderer<Model> {
        override fun render(model: Model) = Unit
      }
      """,
      graphInterfaceSource,
    ) {
      val generatedGraph = testRenderer.rendererGraph

      assertThat(generatedGraph.packageName).startsWith(METRO_LOOKUP_PACKAGE)

      assertThat(generatedGraph.declaredNonSyntheticMethods.map { it.name })
        .containsOnly(
          "provideSoftwareAmazonTestTestRendererModel",
          "provideSoftwareAmazonTestTestRendererModelKey",
        )

      assertThat(graphInterface.newMetroGraph<TestRendererGraph>().renderers.keys)
        .containsOnly(model)
    }
  }

  @Test
  fun `when using @SingleIn(RendererScope_class) then a warning is printed`() {
    compile(
      """
      package software.amazon.test

      import dev.zacsweers.metro.Inject
      import dev.zacsweers.metro.SingleIn
      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import software.amazon.app.platform.renderer.RendererScope

      class Model : BaseModel

      @Inject
      @SingleIn(RendererScope::class)
      @ContributesRenderer
      class TestRenderer(@Suppress("unused") val string: String) : Renderer<Model> {
        override fun render(model: Model) = Unit
      }
      """,
      graphInterfaceSource,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "Source0.kt:15: Renderers should not be singletons in the " +
            "RendererScope. The RendererFactory will cache the Renderer when " +
            "necessary. Remove the @SingleIn(RendererScope::class) annotation."
        )
    }
  }

  @Test
  fun `it is redundant to add @Inject for a zero arg constructor`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer
      import dev.zacsweers.metro.Inject

      class Model : BaseModel

      @ContributesRenderer
      @Inject
      class TestRenderer : Renderer<Model> {
        override fun render(model: Model) = Unit
      }
      """,
      graphInterfaceSource,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "It's redundant to use @Inject when using @ContributesRenderer " +
            "for a Renderer with a zero-arg constructor."
        )
    }
  }

  @Test
  fun `it is required to use @Inject for a non-zero arg constructor`() {
    compile(
      """
      package software.amazon.test

      import software.amazon.app.platform.inject.ContributesRenderer
      import software.amazon.app.platform.presenter.BaseModel
      import software.amazon.app.platform.renderer.Renderer

      class Model : BaseModel

      @ContributesRenderer
      class TestRenderer(@Suppress("unused") val string: String) : Renderer<Model> {
        override fun render(model: Model) = Unit
      }
      """,
      graphInterfaceSource,
      exitCode = COMPILATION_ERROR,
    ) {
      assertThat(messages)
        .contains(
          "When using @ContributesRenderer and you need to inject types " +
            "in the constructor, then it's necessary to add the @Inject annotation."
        )
    }
  }

  @Language("kotlin")
  private val graphInterfaceSource =
    """
        package software.amazon.test

        import software.amazon.app.platform.presenter.BaseModel
        import software.amazon.app.platform.renderer.Renderer
        import software.amazon.app.platform.renderer.RendererScope
        import kotlin.reflect.KClass
        import dev.zacsweers.metro.AppScope
        import dev.zacsweers.metro.createGraph
        import dev.zacsweers.metro.DependencyGraph
        import dev.zacsweers.metro.ForScope
        import dev.zacsweers.metro.Provider
        import dev.zacsweers.metro.Provides
        import dev.zacsweers.metro.SingleIn
        import software.amazon.test.TestRendererGraph

        @DependencyGraph(RendererScope::class)
        @SingleIn(RendererScope::class)
        interface GraphInterface : TestRendererGraph {
            @Provides fun provideString(): String = "abc"

            companion object {
                fun create(): GraphInterface = createGraph<GraphInterface>()
            }
        }
    """

  private val JvmCompilationResult.testRenderer: Class<*>
    get() = classLoader.loadClass("software.amazon.test.TestRenderer")

  private val JvmCompilationResult.model: KClass<out Any>
    get() = classLoader.loadClass("software.amazon.test.Model").kotlin

  private val JvmCompilationResult.presenter: Class<*>
    get() = classLoader.loadClass("software.amazon.test.Presenter")

  private val Class<*>.model: Class<*>
    get() = classes.single { it.simpleName == "Model" }

  private val Class<*>.model1: Class<*>
    get() = classes.single { it.simpleName == "Model1" }

  private val Class<*>.model2: Class<*>
    get() = classes.single { it.simpleName == "Model2" }

  private val Class<*>.rendererGraph: Class<*>
    get() =
      classLoader.loadClass(
        "$METRO_LOOKUP_PACKAGE.$packageName." +
          canonicalName.substringAfter(packageName).substring(1).replace(".", "") +
          "Graph"
      )

  private val Class<*>.modelType: KClass<*>
    get() =
      rendererGraph.declaredNonSyntheticMethods
        .single { it.name == "provideSoftwareAmazonTestTestRendererModel" }
        .getAnnotation(RendererKey::class.java)
        .value
}
