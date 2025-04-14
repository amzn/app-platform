package software.amazon.app.platform.presenter.molecule

import app.cash.molecule.RecompositionMode
import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.test.Test
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import me.tatarka.inject.annotations.Component
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.KmpComponentCreate
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.presenter.PresenterCoroutineScope
import software.amazon.app.platform.presenter.PresenterCoroutineScopeComponent
import software.amazon.app.platform.scope.coroutine.MainCoroutineDispatcher
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

class InjectionTest {

  @Test
  fun `the PresenterCoroutineScope can be injected lazily`() {
    val testScope = CoroutineScope(CoroutineName("TestName"))
    val testDispatcher = Dispatchers.Default

    val component = createTestComponent(testScope, testDispatcher)

    val moleculeScope = component.moleculeScopeFactory.createMoleculeScope()

    assertThat(moleculeScope.coroutineScope.coroutineContext[CoroutineName.Key]?.name)
      .isEqualTo("TestName")

    moleculeScope.cancel()
  }
}

@Component
@SingleIn(AppScope::class)
abstract class TestComponent(
  private val coroutineScope: CoroutineScope,
  private val coroutineDispatcher: CoroutineDispatcher,
) : PresenterCoroutineScopeComponent {
  abstract val moleculeScopeFactory: MoleculeScopeFactory

  @Provides
  @ForScope(AppScope::class)
  fun provideAppScopeCoroutineScope(): CoroutineScope = coroutineScope

  @Provides
  @MainCoroutineDispatcher
  fun provideMainCoroutineDispatcher(): CoroutineDispatcher = coroutineDispatcher

  @Provides
  fun provideMoleculeScopeFactory(factory: TestMoleculeScopeFactory): MoleculeScopeFactory = factory
}

@Inject
@SingleIn(AppScope::class)
class TestMoleculeScopeFactory(
  @PresenterCoroutineScope coroutineScopeFactory: () -> CoroutineScope
) :
  MoleculeScopeFactory by DefaultMoleculeScopeFactory(
    coroutineScopeFactory = coroutineScopeFactory,
    coroutineContext = EmptyCoroutineContext,
    recompositionMode = RecompositionMode.Immediate,
  )

@KmpComponentCreate
expect fun createTestComponent(
  coroutineScope: CoroutineScope,
  coroutineDispatcher: CoroutineDispatcher,
): TestComponent
