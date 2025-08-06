package software.amazon.app.platform.renderer

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.test.ext.junit.rules.ActivityScenarioRule
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotSameInstanceAs
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.isActive
import org.junit.Rule
import org.junit.Test
import software.amazon.app.platform.presenter.BaseModel

class ViewRendererTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @Test
  fun renderModel_is_invoked_for_new_model() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      assertThat(renderer.inflateCalled).isEqualTo(0)
      assertThat(renderer.renderCalled).isEqualTo(0)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun renderModel_is_not_invoked_for_equal_model() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      assertThat(renderer.inflateCalled).isEqualTo(0)
      assertThat(renderer.renderCalled).isEqualTo(0)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)
    }
  }

  @Test
  fun inflate_is_invoked_after_detach() {
    activityRule.scenario.onActivity { activity ->
      val renderer = renderer(activity)

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      activity.contentView.removeAllViews()

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(2)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun inflate_is_not_invoked_after_detach_when_not_released() {
    activityRule.scenario.onActivity { activity ->
      val renderer =
        TestViewRenderer(releaseViewOnDetach = false).also {
          it.init(activity, activity.contentView)
        }

      renderer.render(TestModel(1))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(1)

      activity.contentView.removeAllViews()

      renderer.render(TestModel(2))
      assertThat(renderer.inflateCalled).isEqualTo(1)
      assertThat(renderer.renderCalled).isEqualTo(2)
    }
  }

  @Test
  fun renderModel_provides_the_previous_model() {
    activityRule.scenario.onActivity { activity ->
      var currentModel: TestModel? = null
      var previousModel: TestModel? = null

      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View = View(activity)

          override fun renderModel(model: TestModel, lastModel: TestModel?) {
            currentModel = model
            previousModel = lastModel
          }
        }

      renderer.init(activity, activity.contentView)

      renderer.render(TestModel(1))
      assertThat(currentModel).isEqualTo(TestModel(1))
      assertThat(previousModel).isNull()

      renderer.render(TestModel(2))
      assertThat(currentModel).isEqualTo(TestModel(2))
      assertThat(previousModel).isEqualTo(TestModel(1))

      activity.contentView.removeAllViews()

      renderer.render(TestModel(3))
      assertThat(currentModel).isEqualTo(TestModel(3))
      assertThat(previousModel).isNull()
    }
  }

  @Test
  fun the_coroutine_scope_is_canceled_after_detach() {
    activityRule.scenario.onActivity { activity ->
      lateinit var coroutineScope: CoroutineScope

      val renderer =
        object : ViewRenderer<TestModel>() {
          override fun inflate(
            activity: Activity,
            parent: ViewGroup,
            layoutInflater: LayoutInflater,
            initialModel: TestModel,
          ): View {
            coroutineScope = this.coroutineScope
            return View(activity)
          }
        }

      renderer.init(activity, activity.contentView)

      renderer.render(TestModel(1))
      assertThat(coroutineScope.isActive).isTrue()

      val oldScope = coroutineScope

      activity.contentView.removeAllViews()
      assertThat(coroutineScope.isActive).isFalse()

      renderer.render(TestModel(2))
      assertThat(coroutineScope.isActive).isTrue()
      assertThat(coroutineScope).isNotSameInstanceAs(oldScope)
    }
  }

  private fun renderer(activity: Activity): TestViewRenderer {
    return TestViewRenderer().also { it.init(activity, activity.contentView) }
  }

  private val Activity.contentView: ViewGroup
    get() = findViewById(android.R.id.content)

  private data class TestModel(val value: Int) : BaseModel

  private class TestViewRenderer(private val releaseViewOnDetach: Boolean = true) :
    ViewRenderer<TestModel>() {

    private lateinit var textView: TextView

    var inflateCalled = 0
      private set

    var renderCalled = 0
      private set

    override fun inflate(
      activity: Activity,
      parent: ViewGroup,
      layoutInflater: LayoutInflater,
      initialModel: TestModel,
    ): View =
      TextView(activity).also {
        textView = it
        inflateCalled++
      }

    override fun renderModel(model: TestModel) {
      textView.text = "Test: ${model.value}"
      renderCalled++
    }

    override fun releaseViewOnDetach(): Boolean {
      return releaseViewOnDetach
    }
  }
}
