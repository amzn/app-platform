package software.amazon.app.platform.template.navigation

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import me.tatarka.inject.annotations.Inject
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesBinding
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Default implementation of [ExampleRepository] that holds an integer [StateFlow]
 * and allows its value to be updated.
 *
 * Useful for testing reactive state flow usage with presenters or other consumers.
 */
@Inject
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
class ExampleRepositoryImpl : ExampleRepository {
    private val _exampleStateFlow = MutableStateFlow(0)
    override val exampleStateFlow: StateFlow<Int> = _exampleStateFlow.asStateFlow()

    override fun setExampleFlowValue(value: Int) {
        println("value: $value")
        _exampleStateFlow.value = value
    }
}