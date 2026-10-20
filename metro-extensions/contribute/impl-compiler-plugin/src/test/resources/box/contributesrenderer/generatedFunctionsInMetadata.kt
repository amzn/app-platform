// WITH_REFLECT

package com.test

import kotlin.reflect.full.declaredFunctions
import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer

sealed interface Model : BaseModel {
  data object Child : Model
}

@ContributesRenderer
class TestRenderer : Renderer<Model> {
  override fun render(model: Model) = Unit
}

class InjectModel : BaseModel

@ContributesRenderer
@Inject
class InjectRenderer(val model: InjectModel) : Renderer<InjectModel> {
  override fun render(model: InjectModel) = Unit
}

fun box(): String {
  val classes =
    listOf(
      TestRenderer.RendererContribution::class,
      TestRenderer.RendererContribution.Companion::class,
      InjectRenderer.RendererContribution::class,
      InjectRenderer.RendererContribution.Companion::class,
    )
  val expected =
    listOf(
      setOf("provideComTestTestRendererModel", "provideComTestTestRendererModelChild"),
      setOf(
        "provideComTestTestRenderer",
        "provideComTestTestRendererModelKey",
        "provideComTestTestRendererModelChildKey",
      ),
      setOf("provideComTestInjectRendererInjectModel"),
      setOf("provideComTestInjectRendererInjectModelKey"),
    )
  assertEquals(
    expected = expected,
    actual = classes.map { type ->
      type.java.declaredMethods.filterNot { it.isSynthetic }.map { it.name }.toSet()
    },
    message = "Generated functions must exist in JVM bytecode",
  )
  assertEquals(
    expected = expected,
    actual = classes.map { type -> type.declaredFunctions.map { it.name }.toSet() },
    message = "Generated functions must exist in Kotlin metadata",
  )
  return "OK"
}
