package software.amazon.app.platform.recipes

import android.content.Context
import android.os.Bundle
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.viewModels
import co.touchlab.kermit.Logger
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.forwardBackPressEventsToPresenters
import software.amazon.app.platform.recipes.app.R
import software.amazon.app.platform.recipes.app.databinding.FeatureContainerBinding
import software.amazon.app.platform.recipes.app.databinding.WorkflowContainerBinding
import software.amazon.app.platform.renderer.AndroidRendererFactory
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.kotlinInjectComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import kotlin.getValue

/**
 * Ideally apps should have a single hierarchy, but it is not always the case. This activity
 * represents a feature that launches from another activity.
 */
class FeatureActivity : AppCompatActivity() {
  private lateinit var binding: FeatureContainerBinding

//  override fun onCreate(savedInstanceState: Bundle?) {
//    super.onCreate(savedInstanceState)
//
//    Logger.i { "[jesslwan] Create feature activity"}
//
//    binding = FeatureContainerBinding.inflate(layoutInflater).also { setContentView(it.root) }
//
//    supportFragmentManager
//      .beginTransaction()
//      .add(binding.featureContainer.id, FeatureFragment())
//      .commit()
//  }

  override fun onDestroy() {
    Logger.i { "[jesslwan] Destroy feature activity"}
    super.onDestroy()
  }
  private val rootScopeProvider
    get() = application as RootScopeProvider

  private val viewModelFactory by viewModels<ViewModelFactory> {
    ViewModelFactory.factory(scopeName = this::class.java.simpleName)
  }

  private val viewModel: MainActivityViewModel by viewModels { viewModelFactory }

  private val component
    get() = rootScopeProvider.rootScope.kotlinInjectComponent<Component>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    Logger.i { "[jesslwan] Create feature activity"}

    component.backGestureDispatcherPresenter.forwardBackPressEventsToPresenters(this)

    val binding = WorkflowContainerBinding.inflate(layoutInflater)
    setContentView(binding.root)

    val rendererFactory = AndroidRendererFactory(rootScopeProvider, this, binding.workflowContainer)

    component.templateEngineFactory.create(rendererFactory).take(
      templates = viewModel.templates,
      lifecycle = lifecycle,
    )
  }

  @ContributesTo(AppScope::class)
  interface Component {
    val backGestureDispatcherPresenter: BackGestureDispatcherPresenter
    val templateEngineFactory: TemplateEngine.Factory
  }

}
