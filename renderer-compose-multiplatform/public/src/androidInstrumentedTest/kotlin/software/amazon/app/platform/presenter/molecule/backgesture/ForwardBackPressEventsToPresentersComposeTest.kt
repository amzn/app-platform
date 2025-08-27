package software.amazon.app.platform.presenter.molecule.backgesture

import androidx.activity.compose.setContent
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.Rule
import org.junit.Test
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresentersComposeTest.TestPresenter.Model
import software.amazon.app.platform.presenter.molecule.returningCompositionLocalProvider
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.TestActivity
import software.amazon.app.platform.renderer.getActivityFromTestRule

class ForwardBackPressEventsToPresentersComposeTest {

  @get:Rule val activityRule = ActivityScenarioRule(TestActivity::class.java)

  @get:Rule val composeTestRule = AndroidComposeTestRule(activityRule, ::getActivityFromTestRule)

  @Test
  fun back_press_events_are_forwarded_to_presenters() {
    val backGestureDispatcherPresenter = BackGestureDispatcherPresenter.createNewInstance()

    val testPresenter = TestPresenter(backGestureDispatcherPresenter)
    val testRenderer = TestRenderer()
    val rootRenderer = RootRenderer(backGestureDispatcherPresenter, testRenderer)

    activityRule.scenario.onActivity { activity ->
      activity.setContent {
        val model = testPresenter.present(Unit)
        rootRenderer.renderCompose(model)
      }
    }

    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 0")

    Espresso.pressBack()
    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 1")

    Espresso.pressBack()
    composeTestRule.onNodeWithTag("count").assertTextEquals("Count: 2")
  }

  private class RootRenderer(
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
    private val testRenderer: TestRenderer,
  ) : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
      backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

      testRenderer.renderCompose(model)
    }
  }

  private class TestPresenter(
    private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter
  ) : MoleculePresenter<Unit, Model> {
    @Composable
    override fun present(input: Unit): Model {
      return returningCompositionLocalProvider(
        LocalBackGestureDispatcherPresenter provides backGestureDispatcherPresenter
      ) {
        var backPressCount by remember { mutableIntStateOf(0) }

        BackHandlerPresenter { backPressCount++ }

        Model(backPressCount = backPressCount)
      }
    }

    data class Model(val backPressCount: Int) : BaseModel
  }

  private class TestRenderer : ComposeRenderer<Model>() {
    @Composable
    override fun Compose(model: Model) {
      BasicText(text = "Count: ${model.backPressCount}", modifier = Modifier.testTag("count"))
    }
  }
}
