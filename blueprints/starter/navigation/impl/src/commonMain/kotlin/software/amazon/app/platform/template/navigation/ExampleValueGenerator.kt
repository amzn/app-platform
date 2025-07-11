package software.amazon.app.platform.template.navigation

import kotlinx.coroutines.delay
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.Scoped
import software.amazon.app.platform.scope.coroutine.launch
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * A scoped service that continuously generates random values and feeds them into [ExampleRepository]
 * every 3 seconds. This is active only while the [AppScope] is alive.
 *
 * This class is:
 * - Bound to [AppScope] via `@ContributesBinding`
 * - A singleton within that scope via `@SingleIn`
 * - Injected via constructor using `@Inject`
 *
 * The generator starts emitting random integers in the range 1 to 100 as soon as the scope is entered.
 *
 * @property exampleRepository the repository where generated values are pushed
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ExampleValueGenerator(
    private val exampleRepository: ExampleRepository,
) : Scoped {
    override fun onEnterScope(scope: Scope) {
        scope.launch {
            while (true) {
                val random = (1..100).random()
                println("random: $random")
                exampleRepository.setExampleFlowValue(random)
                delay(3000L)
            }
        }

    }
}