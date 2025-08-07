package sample

import android.os.Looper
import assertk.assertThat
import assertk.assertions.isNull
import kotlin.test.Test

class AndroidTest {
  @Test
  fun `android jar test`() {
    assertThat(Looper.getMainLooper()).isNull()
  }
}
