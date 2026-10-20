package com.test

import software.ralf.app.platform.inject.ContributesRenderer
import software.ralf.app.platform.renderer.Renderer

@ContributesRenderer
class TestRenderer : Renderer<Model> {
  override fun render(model: Model) = Unit
}
