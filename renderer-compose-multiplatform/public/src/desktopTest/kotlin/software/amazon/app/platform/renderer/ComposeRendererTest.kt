package software.amazon.app.platform.renderer

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.runComposeUiTest
import kotlin.test.Test
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assume.assumeFalse
import software.amazon.app.platform.BuildConfig.IS_CI
import software.amazon.app.platform.presenter.BaseModel

@Suppress("MagicNumber")
@ExperimentalTestApi
class ComposeRendererTest {

  @Test
  fun `a model is rendered with Compose UI elements`() {
    // During dry-run builds the native binaries for Compose aren't available, so ignore this
    // test in CI for now. We still want to run it locally though.
    //
    // java.lang.UnsatisfiedLinkError:
    // /home/p4admin/.skiko/c7e8e6b2bfb3a52eb4cd32174de30725cdf585d44271d2c576db6b19719e95f2/libskiko-linux-x64.so:
    // libGL.so.1: cannot open shared object file: No such file or directory
    assumeFalse(IS_CI)

    runComposeUiTest {
      val renderer = TestRenderer()
      val models = MutableStateFlow(Model(1))

      setContent {
        val model by models.collectAsState()
        renderer.renderCompose(model)
      }

      onNodeWithTag("text").assertTextEquals("Argument 1")

      models.value = Model(2)
      onNodeWithTag("text").assertTextEquals("Argument 2")

      models.value = Model(3)
      onNodeWithTag("text").assertTextEquals("Argument 3")
    }
  }

  @Test
  fun `a ComposeRenderer can nest other ComposeRenderers`() {
    // During dry-run builds the native binaries for Compose aren't available, so ignore this
    // test in CI for now. We still want to run it locally though.
    //
    // java.lang.UnsatisfiedLinkError:
    // /home/p4admin/.skiko/c7e8e6b2bfb3a52eb4cd32174de30725cdf585d44271d2c576db6b19719e95f2/libskiko-linux-x64.so:
    // libGL.so.1: cannot open shared object file: No such file or directory
    assumeFalse(IS_CI)

    runComposeUiTest {
      val renderer = OuterRenderer(TestRenderer())
      val models = MutableStateFlow(Model(1))

      setContent {
        val model by models.collectAsState()
        renderer.renderCompose(model)
      }

      onNodeWithTag("text").assertTextEquals("Argument 1")
      onNodeWithTag("text-outer").assertTextEquals("Outer 1")

      models.value = Model(2)
      onNodeWithTag("text").assertTextEquals("Argument 2")
      onNodeWithTag("text-outer").assertTextEquals("Outer 2")
    }
  }

  private data class Model(val value: Int) : BaseModel

  private class TestRenderer : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
      Text(text = "Argument ${model.value}", modifier = Modifier.testTag("text"))
    }
  }

  private class OuterRenderer(private val testRenderer: TestRenderer) : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
      Column {
        Text(text = "Outer ${model.value}", modifier = Modifier.testTag("text-outer"))
        testRenderer.renderCompose(model)
      }
    }
  }
}
