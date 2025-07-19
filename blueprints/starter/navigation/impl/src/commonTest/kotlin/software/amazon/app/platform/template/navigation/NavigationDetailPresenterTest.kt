package software.amazon.app.platform.template.navigation

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlin.test.Test
import kotlinx.coroutines.test.runTest
import software.amazon.app.platform.presenter.molecule.test
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class NavigationDetailPresenterTest {

  @Test
  fun `model changes when setExampleFlowValue is called`() = runTest {
    val exampleRepository = FakeExampleRepository()
    NavigationDetailPresenterImpl(exampleRepository).test(this) {
      awaitItem().let { model ->
        assertThat(model.exampleValue).isEqualTo(0)
        assertThat(model.exampleCount).isEqualTo(0)
      }

      exampleRepository.setExampleFlowValue(5)

      awaitItem().let { model ->
        assertThat(model.exampleValue).isEqualTo(5)
      }

      // There is a 1 milli delay within presenter before updating count.
      advanceTimeBy(1.milliseconds)

      awaitItem().let { model ->
        assertThat(model.exampleValue).isEqualTo(5)
        assertThat(model.exampleCount).isEqualTo(1)
      }
    }
  }
}
