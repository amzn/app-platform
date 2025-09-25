package software.amazon.app.platform.recipes

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import software.amazon.app.platform.recipes.app.databinding.FeatureContainerBinding

/**
 * Ideally apps should have a single hierarchy, but it is not always the case. This activity represents
 * a feature that launches from another activity.
 */
class FeatureActivity : AppCompatActivity() {
  private lateinit var binding: FeatureContainerBinding

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    binding =
      FeatureContainerBinding.inflate(layoutInflater).also {
        setContentView(it.root)
      }

    supportFragmentManager.beginTransaction().add(
      binding.featureContainer.id,
      FeatureFragment(),
    ).commit()
  }
}
