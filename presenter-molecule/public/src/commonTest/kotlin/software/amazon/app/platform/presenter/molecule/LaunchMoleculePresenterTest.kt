package software.amazon.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.startsWith
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.IO
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.IgnoreNative
import software.amazon.app.platform.currentThreadName
import software.amazon.app.platform.presenter.BaseModel

@OptIn(ExperimentalCoroutinesApi::class)
class LaunchMoleculePresenterTest {

  @Test
  @IgnoreNative
  fun `the first present call happens inline and the second present call happens on the background thread`() =
    runTest {
      val inputFlow = MutableStateFlow("1")
      TestPresenter().test(this, inputFlow, UnconfinedTestDispatcher()) {
        val model1 = awaitItem()
        inputFlow.value = "2"
        val model2 = awaitItem()

        val testRunnerPackage = "kotlinx.coroutines.test"
        assertThat(model1.threadName).startsWith("Test worker")
        assertThat(model1.threadName).contains(testRunnerPackage)
        assertThat(model2.threadName).startsWith("Test worker")
        assertThat(model2.threadName).doesNotContain(testRunnerPackage)
      }
    }

  @Test
  fun `the presenter is called and computes a new model whenever the input changes`() = runTest {
    data class Model(val value: String) : BaseModel

    val presenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          return Model(input.toString())
        }
      }

    val inputFlow = MutableStateFlow(1)

    presenter.test(this, inputFlow) {
      assertThat(awaitItem().value).isEqualTo("1")

      inputFlow.value = 2
      assertThat(awaitItem().value).isEqualTo("2")

      inputFlow.value = 3
      assertThat(awaitItem().value).isEqualTo("3")
    }
  }

  @Test
  fun `launching a presenter on a canceled scope throws an error`() = runTest {
    val coroutineScope = CoroutineScope(Dispatchers.IO)
    coroutineScope.cancel()
    assertThat(coroutineScope.isActive).isFalse()

    val moleculeScope = MoleculeScope(coroutineScope, RecompositionMode.Immediate)

    data class Model(val value: String) : BaseModel

    val presenter =
      object : MoleculePresenter<Int, Model> {
        @Composable
        override fun present(input: Int): Model {
          return Model(input.toString())
        }
      }

    assertFailsWith<IllegalStateException> { moleculeScope.launchMoleculePresenter(presenter, 1) }
  }

  private class TestPresenter : MoleculePresenter<StateFlow<String>, TestPresenter.Model> {
    @Composable
    override fun present(input: StateFlow<String>): Model {
      val value by input.collectAsState()

      return Model(threadName = currentThreadName + value)
    }

    data class Model(val threadName: String) : BaseModel
  }
}
