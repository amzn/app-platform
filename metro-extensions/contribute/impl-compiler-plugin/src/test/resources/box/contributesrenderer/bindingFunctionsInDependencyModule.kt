// MODULE: lib
// FILE: TestRenderer.kt
package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.presenter.BaseModel
import software.ralf.app.platform.renderer.Renderer

class Model : BaseModel

@ContributesRenderer
class TestRenderer : Renderer<Model> {
  override fun render(model: Model) = Unit
}

// MODULE: main(lib)
// FILE: main.kt
package com.test

import software.ralf.app.platform.renderer.Renderer

fun bindRenderer(
  contribution: TestRenderer.RendererContribution,
  renderer: TestRenderer,
): Renderer<*> = contribution.provideComTestTestRendererModel(renderer)

fun box(): String {
  val renderer = TestRenderer.RendererContribution.provideComTestTestRenderer()
  renderer.render(Model())
  assertEquals(
    expected = TestRenderer::class,
    actual = TestRenderer.RendererContribution.provideComTestTestRendererModelKey(),
  )
  return "OK"
}
