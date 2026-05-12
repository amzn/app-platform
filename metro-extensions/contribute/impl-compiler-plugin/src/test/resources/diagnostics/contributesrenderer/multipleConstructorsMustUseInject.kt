// RENDER_DIAGNOSTICS_FULL_TEXT
package com.test

import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.Renderer

class Model : BaseModel

<!CONTRIBUTES_RENDERER_ERROR!>@ContributesRenderer<!>
class TestRenderer(val string: String) : Renderer<Model> {
  constructor(string: String, marker: String) : this(string)

  override fun render(model: Model) = Unit
}
