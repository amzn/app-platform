@file:OptIn(ExperimentalMaterial3Api::class)

package software.amazon.app.platform.recipes.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.zacsweers.metro.Inject
import software.amazon.app.platform.inject.metro.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.molecule.backgesture.BackGestureDispatcherPresenter
import software.amazon.app.platform.presenter.molecule.backgesture.ForwardBackPressEventsToPresenters
import software.amazon.app.platform.recipes.appbar.AppBarConfig
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.Renderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

/**
 * A Compose renderer implementation for templates used in the recipes application.
 *
 * [rendererFactory] is used to get the [Renderer] for the [BaseModel] wrapped in the template.
 */
@Inject
@ContributesRenderer
class RootPresenterRenderer(
  private val rendererFactory: RendererFactory,
  private val backGestureDispatcherPresenter: BackGestureDispatcherPresenter,
) : ComposeRenderer<RecipesAppTemplate>() {
  @Composable
  override fun Compose(model: RecipesAppTemplate) {
    backGestureDispatcherPresenter.ForwardBackPressEventsToPresenters()

    when (model) {
      is RecipesAppTemplate.FullScreenTemplate -> FullScreen(model)
    }
  }

  @Composable
  private fun FullScreen(template: RecipesAppTemplate.FullScreenTemplate) {
    CenterAlignedTopAppBar(template.appBarConfig) {
      Box(modifier = Modifier.padding(it)) {
        val renderer = rendererFactory.getComposeRenderer(template.model)
        renderer.renderCompose(template.model)
      }
    }
  }

  @Composable
  private fun CenterAlignedTopAppBar(
    appBarConfig: AppBarConfig,
    content: @Composable (PaddingValues) -> Unit,
  ) {
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        CenterAlignedTopAppBar(
          colors =
            TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.primaryContainer,
              titleContentColor = MaterialTheme.colorScheme.primary,
            ),
          title = { Text(appBarConfig.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
          navigationIcon = {
            if (appBarConfig.backArrowAction != null) {
              IconButton(onClick = appBarConfig.backArrowAction) {
                Icon(
                  imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                  contentDescription = "Localized description",
                )
              }
            }
          },
          actions = {
            if (appBarConfig.menuItems.isNotEmpty()) {
              MinimalDropdownMenu(appBarConfig.menuItems)
            }
          },
          scrollBehavior = scrollBehavior,
        )
      },
    ) { innerPadding ->
      content(innerPadding)
    }
  }

  @Composable
  private fun MinimalDropdownMenu(menuItems: List<AppBarConfig.MenuItem>) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = Modifier.padding(16.dp)) {
      IconButton(onClick = { expanded = !expanded }) {
        Icon(imageVector = Icons.Filled.MoreVert, contentDescription = "More options")
      }
      DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
        menuItems.forEach { item ->
          DropdownMenuItem(
            text = { Text(item.text) },
            onClick = {
              expanded = false
              item.action()
            },
          )
        }
      }
    }
  }

  //  * @ContributesTo(RendererScope::class)
  // * interface IncrementRendererComponent {
  // *     @Provides
  // *     @IntoMap
  // *     fun provideIncrementRendererIncrementPresenterModel(
  // *         renderer: () -> IncrementRenderer,
  // *     ): Pair<KClass<out BaseModel>, () -> Renderer<*>> = IncrementPresenter.Model::class to renderer
  // *
  // *     @Provides
  // *     fun provideIncrementRenderer(): IncrementRenderer = IncrementRenderer()
  // * }

  //  *     @Provides
  // *     @IntoMap
  // *     @ForScope(RendererScope::class)
  // *     fun provideRendererModelKey(): Pair<KClass<out BaseModel>, KClass<out Renderer<*>>> =
  // *         Model::class to TestRenderer::class

//  @ContributesTo(RendererScope::class)
//  interface Component {
//    @Provides
//    @IntoMap
//    @RendererKey(RecipesAppTemplate::class)
//    fun provideRenderer(
//      renderer: Provider<RootPresenterRenderer>
//    ): Renderer<*> = renderer.invoke()
//
//    @Provides
//    @IntoMap
//    @RendererKey(RecipesAppTemplate.FullScreenTemplate::class)
//    fun provideRenderer2(
//      renderer: Provider<RootPresenterRenderer>
//    ): Renderer<*> = renderer.invoke()
//
//    @Provides
//    @IntoMap
//    @ForScope(RendererScope::class)
//    @RendererKey(RecipesAppTemplate::class)
//    fun provideRendererMapping(
////      renderer: Provider<RootPresenterRenderer>
//    ): KClass<out Renderer<*>> = RootPresenterRenderer::class
//
//    @Provides
//    @IntoMap
//    @ForScope(RendererScope::class)
//    @RendererKey(RecipesAppTemplate.FullScreenTemplate::class)
//    fun provideRendererMapping2(
////      renderer: Provider<RootPresenterRenderer>
//    ): KClass<out Renderer<*>> = RootPresenterRenderer::class
//  }
}

//@Target(
//  AnnotationTarget.FUNCTION,
//  AnnotationTarget.FIELD,
//  AnnotationTarget.PROPERTY,
//  AnnotationTarget.PROPERTY_GETTER,
//  AnnotationTarget.CLASS,
//  AnnotationTarget.TYPE,
//)
//@Retention(AnnotationRetention.RUNTIME)
//@MapKey
//public annotation class RendererKey(val value: KClass<*>)

