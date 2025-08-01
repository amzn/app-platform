@file:Suppress("UndocumentedPublicProperty", "UndocumentedPublicClass")

package software.amazon.app.platform.recipes.nav3

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshots.SnapshotStateList
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.MoleculePresenter
import software.amazon.app.platform.recipes.nav3.Navigation3ChildPresenter.Model

class Navigation3ChildPresenter(
  private val index: Int,
  private val backstack: SnapshotStateList<MoleculePresenter<Unit, out BaseModel>>,
) : MoleculePresenter<Unit, Model> {
  @Composable
  override fun present(input: Unit): Model {
    val color = remember { nextColor() }

    val counter by
      produceState(0) {
        while (isActive) {
          delay(1.seconds)
          value += 1
        }
      }

    return Model(index = index, color = color, counter = counter) {
      when (it) {
        Event.AddPresenter ->
          backstack.add(Navigation3ChildPresenter(index = index + 1, backstack = backstack))
      }
    }
  }

  data class Model(
    val index: Int,
    val color: Long,
    val counter: Int,
    val onEvent: (Event) -> Unit,
  ) : BaseModel

  sealed interface Event {
    data object AddPresenter : Event
  }

  private companion object {
    private val colors =
      listOf(0xFFA5D6A7, 0xFF81D4FA, 0xFFB0BEC5, 0xFFBCAAA4, 0xFF80CBC4, 0xFFFFAB91)

    private var index = 0

    fun nextColor(): Long {
      index = (index + 1).mod(colors.size)
      return colors[index]
    }
  }
}
