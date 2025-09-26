package software.amazon.app.platform.recipes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.forwardBackPressEventsToPresenters
import software.amazon.app.platform.recipes.MainActivityViewModel.Component
import software.amazon.app.platform.recipes.app.R
import software.amazon.app.platform.renderer.AndroidRendererFactory
import software.amazon.app.platform.renderer.getRenderer
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.di.kotlinInjectComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

class FeatureFragment : Fragment() {
  private val rootScopeProvider
    get() = requireActivity().application as RootScopeProvider

  private val viewModel by viewModels<MainActivityViewModel>()

  private val component
    get() = rootScopeProvider.rootScope.kotlinInjectComponent<Component>()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    component.backGestureDispatcherPresenter.forwardBackPressEventsToPresenters(requireActivity())
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?,
  ): View {
    return inflater.inflate(R.layout.workflow_container, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)

    val parentView = view.findViewById<ViewGroup>(R.id.workflow_container)

    val rendererFactory = AndroidRendererFactory(rootScopeProvider, requireActivity(), parentView)

    val templates = viewModel.templates

    viewLifecycleOwner.lifecycleScope.launch {
      templates.collect { template ->
        val renderer = rendererFactory.getRenderer(template)
        renderer.render(template)
      }
    }
  }

  @ContributesTo(AppScope::class)
  interface Component {
    val backGestureDispatcherPresenter: BackGestureDispatcherPresenter
  }
}
