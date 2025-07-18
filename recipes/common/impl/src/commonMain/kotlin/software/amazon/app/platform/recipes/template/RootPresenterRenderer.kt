@file:OptIn(ExperimentalMaterial3Api::class)

package software.amazon.app.platform.recipes.template

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextOverflow
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
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
class RootPresenterRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<RecipesAppTemplate>() {
  @Composable
  override fun Compose(model: RecipesAppTemplate) {
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
          //          actions = {
          //            IconButton(onClick = { /* do something */ }) {
          //              Icon(
          //                imageVector = Icons.Filled.Menu,
          //                contentDescription = "Localized description"
          //              )
          //            }
          //          },
          scrollBehavior = scrollBehavior,
        )
      },
    ) { innerPadding ->
      content(innerPadding)
    }
  }
}
