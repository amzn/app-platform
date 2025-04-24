package software.amazon.app.platform.presenter.molecule

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import app.cash.molecule.RecompositionMode
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isSameInstanceAs
import assertk.assertions.isTrue
import assertk.fail
import kotlin.test.Test
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.plus
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import software.amazon.app.platform.presenter.BaseModel

@Suppress("DEPRECATION")
@OptIn(ExperimentalCoroutinesApi::class)
class OnEventTest {

  @Test
  fun `onEvent remembers lambdas to implement equals properly`() = runTest {
    val inputFlow = MutableStateFlow("1")
    CoroutineNamePresenter().test(this, inputFlow) {
      val model1 = awaitItem()
      inputFlow.value = "2"
      val model2 = awaitItem()

      assertThat(model1.onEvent).isSameInstanceAs(model2.onEvent)
    }
  }

  @Test
  fun `onEvent is invoked on dispatcher running the presenter`() = runTest {
    val inputFlow = MutableStateFlow("1")
    CoroutineNamePresenter().test(this, inputFlow, CoroutineName("UniqueName")) {
      awaitItem().onEvent(CoroutineNamePresenter.Event.AnyEvent)
      val coroutineName = awaitItem().coroutineName

      assertThat(coroutineName).contains("UniqueName")
    }
  }

  @Test
  fun `onEvent is not invoked after the presenter leaves composition`() = runBlocking {
    val scope1 = CoroutineScope(coroutineContext + Job())
    val scope2 = CoroutineScope(coroutineContext + Job())

    try {
      val onEventCount = Wrapper(0)
      val presenter = OnEventNotInvokedPresenter(onEventCount)
      lateinit var delayedModel: OnEventNotInvokedPresenter.Model

      MoleculeScope(scope1, RecompositionMode.Immediate)
        .launchMoleculePresenter(presenter, Unit)
        .model
        .test {
          val model = awaitItem()
          assertThat(model.onEventInvoked).isEqualTo(0)
          assertThat(onEventCount.value).isEqualTo(0)

          model.onEvent(OnEventNotInvokedPresenter.Event.AnyEvent)

          delayedModel = awaitItem()
          assertThat(delayedModel.onEventInvoked).isEqualTo(1)
          assertThat(onEventCount.value).isEqualTo(1)
        }

      // Cancel the presenter. That's the equivalent of leaving the composition and events
      // should not be forwarded anymore.
      scope1.cancel()

      // Verify the event wasn't delivered anymore and our counter wasn't updated.
      delayedModel.onEvent(OnEventNotInvokedPresenter.Event.AnyEvent)
      assertThat(onEventCount.value).isEqualTo(1)

      // Launch the same presenter again (enters composition).
      MoleculeScope(scope2, RecompositionMode.Immediate)
        .launchMoleculePresenter(presenter, Unit)
        .model
        .test {
          val initialModel = awaitItem()
          // It's a new composition, therefore the onEventInvoked is 0 again.
          assertThat(initialModel.onEventInvoked).isEqualTo(0)
          // The old counter value wasn't updated yet, hence it's 1.
          assertThat(onEventCount.value).isEqualTo(1)

          initialModel.onEvent(OnEventNotInvokedPresenter.Event.AnyEvent)

          assertThat(awaitItem().onEventInvoked).isEqualTo(1)
          assertThat(onEventCount.value).isEqualTo(2)
        }
    } finally {
      scope1.cancel()
      scope2.cancel()
    }
  }

  @Test
  fun `invoking onEvent on an outdated model is supported`() = runTest {
    val onEventCount = Wrapper(0)
    OnEventNotInvokedPresenter(onEventCount).test(this) {
      val initialModel = awaitItem()
      assertThat(initialModel.onEventInvoked).isEqualTo(0)
      assertThat(onEventCount.value).isEqualTo(0)

      val producedModels = mutableListOf(initialModel)

      repeat(10) { index ->
        // Notice that always the same model is used for sending events. That's
        // not general best practice, but must work as expected.
        initialModel.onEvent(OnEventNotInvokedPresenter.Event.AnyEvent)

        val model = awaitItem()
        producedModels += model

        assertThat(model.onEventInvoked).isEqualTo(index + 1)
        assertThat(onEventCount.value).isEqualTo(index + 1)
      }

      assertThat(producedModels.distinct()).hasSize(11)
    }
  }

  @Test
  fun `the last used lambda is invoked with the freshest data no matter which model is used to invoke the event handler`() =
    runTest {
      // When onEvent is called a new lambda is created and passed in as argument for each
      // composition. This test verifies that always the latest lambda is invoked. This is
      // crucial, because the lambdas capture the state within the `present()` call and old
      // lambdas reference stale data.

      val counter = Wrapper(0)
      val trigger = MutableStateFlow(1)
      var composeTrigger = -1
      var counterValue = -1

      val lambda1 = { _: Int, _: Int -> fail("lambda1 should not be invoked") }
      val lambda2 = { _: Int, _: Int -> fail("lambda2 should not be invoked") }
      var lambda3Called = false
      val lambda3 = { composeTriggerCallback: Int, counterValueCallback: Int ->
        lambda3Called = true
        composeTrigger = composeTriggerCallback
        counterValue = counterValueCallback
      }

      NewLambdaPresenter(counter, trigger, lambda1, lambda2, lambda3).test(this) {
        val model1 = awaitItem()
        assertThat(model1.composeTrigger).isEqualTo(1)
        assertThat(model1.counterValue).isEqualTo(1)
        assertThat(lambda3Called).isFalse()

        trigger.value = 2

        val model2 = awaitItem()
        assertThat(model2.composeTrigger).isEqualTo(2)
        assertThat(model2.counterValue).isEqualTo(2)
        assertThat(lambda3Called).isFalse()

        trigger.value = 3

        val model3 = awaitItem()
        assertThat(model3.composeTrigger).isEqualTo(3)
        assertThat(model3.counterValue).isEqualTo(3)
        assertThat(lambda3Called).isFalse()

        // Note that model1 is used to send a UI event back. This happens frequently in
        // practice, e.g. the ViewRenderer checks models for equality and does not "render" the
        // new model if the content is the same. The render then invokes callbacks on an
        // older model.
        model1.onEvent(NewLambdaPresenter.Event)

        runCurrent()
        assertThat(lambda3Called).isTrue()

        assertThat(composeTrigger).isEqualTo(3)
        assertThat(counterValue).isEqualTo(3)
      }
    }

  @Test
  fun `onEvent can be invoked immediately for first event`() = runTest {
    val trigger = MutableStateFlow(1)

    var lambda1Called = false
    val lambda1 = { _: Int, _: Int -> lambda1Called = true }
    val lambda2 = { _: Int, _: Int -> fail("lambda1 should not be invoked") }
    val lambda3 = { _: Int, _: Int -> fail("lambda2 should not be invoked") }

    val moleculeScope = MoleculeScope(backgroundScope, RecompositionMode.Immediate)
    val presenter = NewLambdaPresenter(Wrapper(0), trigger, lambda1, lambda2, lambda3)

    val models = moleculeScope.launchMoleculePresenter(presenter, Unit).model
    models.value.onEvent(NewLambdaPresenter.Event)

    // The yield() is important and acceptable. The test is not verifying that the lambda
    // is called inline. Compose (Molecule) doesn't allow that with the StandardTestDispatcher.
    // This test verifies that the lambda is called at all.
    yield()

    assertThat(lambda1Called).isTrue()
  }

  @Test
  fun `onEvent can be invoked immediately for second event`() = runTest {
    val trigger = MutableStateFlow(1)

    val lambda1 = { _: Int, _: Int -> fail("lambda1 should not be invoked") }
    var lambda2Called = false
    val lambda2 = { _: Int, _: Int -> lambda2Called = true }
    val lambda3 = { _: Int, _: Int -> fail("lambda2 should not be invoked") }

    val moleculeScope = MoleculeScope(backgroundScope, RecompositionMode.Immediate)
    val presenter = NewLambdaPresenter(Wrapper(0), trigger, lambda1, lambda2, lambda3)

    val models = moleculeScope.launchMoleculePresenter(presenter, Unit).model

    val onEventCalled = async {
      val model = models.drop(1).first()

      model.onEvent(NewLambdaPresenter.Event)
    }

    assertThat(lambda2Called).isFalse()

    // That should invoke lambda2 by emitting a new model.
    trigger.value = 2
    onEventCalled.await()

    assertThat(lambda2Called).isTrue()
  }

  @Test
  fun `onEvent can be invoked from nested results`() = runTest {
    var onEventCalled = false

    data class Model(val onEvent: (Unit) -> Unit) : BaseModel

    class InnerPresenter : MoleculePresenter<Unit, Model> {
      @Composable
      override fun present(input: Unit): Model {
        return Model(onEvent = onEvent { onEventCalled = true })
      }
    }

    val innerPresenter = InnerPresenter()

    class OuterPresenter : MoleculePresenter<Unit, BaseModel> {
      @Composable
      override fun present(input: Unit): BaseModel {
        innerPresenter.present(Unit).onEvent(Unit)
        return object : BaseModel {}
      }
    }

    val outerPresenter = OuterPresenter()

    val moleculeScopeStandard = MoleculeScope(backgroundScope, RecompositionMode.Immediate)
    val moleculeScopeUnconfined =
      MoleculeScope(backgroundScope + UnconfinedTestDispatcher(), RecompositionMode.Immediate)

    with(moleculeScopeStandard) {
      val models = launchMoleculePresenter(outerPresenter, Unit).model
      assertThat(models.value).isNotNull()
      assertThat(onEventCalled).isFalse()

      // The yield() is important and acceptable. The test is not verifying that the lambda
      // is called inline. Compose (Molecule) doesn't allow that with the
      // StandardTestDispatcher. This test verifies that the lambda is called at all.
      yield()

      assertThat(onEventCalled).isTrue()
    }

    onEventCalled = false

    with(moleculeScopeUnconfined) {
      assertThat(onEventCalled).isFalse()

      val models = launchMoleculePresenter(outerPresenter, Unit).model
      assertThat(models.value).isNotNull()

      assertThat(onEventCalled).isTrue()
    }
  }

  private class CoroutineNamePresenter :
    MoleculePresenter<StateFlow<String>, CoroutineNamePresenter.Model> {

    @Composable
    override fun present(input: StateFlow<String>): Model {
      val value by input.collectAsState()
      var coroutineName by remember { mutableStateOf("default") }

      return Model(
        value = value,
        coroutineName = coroutineName,
        onEvent = onEvent { coroutineName = currentCoroutineContext()[CoroutineName]!!.name },
      )
    }

    data class Model(val value: String, val coroutineName: String, val onEvent: (Event) -> Unit) :
      BaseModel

    sealed interface Event {
      data object AnyEvent : Event
    }
  }

  private class OnEventNotInvokedPresenter(private val onEventCount: Wrapper<Int>) :
    MoleculePresenter<Unit, OnEventNotInvokedPresenter.Model> {
    @Composable
    override fun present(input: Unit): Model {
      var onEventInvoked by remember { mutableStateOf(0) }
      return Model(
        onEventInvoked = onEventInvoked,
        onEvent =
          onEvent {
            onEventInvoked++
            onEventCount.value++
          },
      )
    }

    data class Model(val onEventInvoked: Int, val onEvent: (Event) -> Unit) : BaseModel

    sealed interface Event {
      data object AnyEvent : Event
    }
  }

  private class NewLambdaPresenter(
    private val counter: Wrapper<Int>,
    private val composeTrigger: StateFlow<Int>,
    private val lambda1: (Int, Int) -> Unit,
    private val lambda2: (Int, Int) -> Unit,
    private val lambda3: (Int, Int) -> Unit,
  ) : MoleculePresenter<Unit, NewLambdaPresenter.Model> {
    @Composable
    override fun present(input: Unit): Model {
      val composeTrigger by composeTrigger.collectAsState()

      counter.value++
      val counterValue = counter.value

      val lambda =
        when (counter.value) {
          1 -> lambda1
          2 -> lambda2
          3 -> lambda3
          else -> throw NotImplementedError()
        }

      return Model(
        composeTrigger = composeTrigger,
        counterValue = counterValue,
        onEvent = onEvent { lambda.invoke(composeTrigger, counterValue) },
      )
    }

    data class Model(val composeTrigger: Int, val counterValue: Int, val onEvent: (Event) -> Unit) :
      BaseModel

    object Event
  }

  private class Wrapper<T : Any>(var value: T)
}
