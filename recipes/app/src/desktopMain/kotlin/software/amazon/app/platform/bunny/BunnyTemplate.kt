@file:OptIn(ExperimentalSharedTransitionApi::class)

package software.amazon.app.platform.bunny

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import me.tatarka.inject.annotations.Inject
import software.amazon.app.platform.bunny.AnimationContentKey.Companion.contentKey
import software.amazon.app.platform.bunny.layer.AboutMenuModel
import software.amazon.app.platform.bunny.layer.ActionButtonModel
import software.amazon.app.platform.bunny.layer.BaseContainerModel
import software.amazon.app.platform.bunny.layer.EtaModel
import software.amazon.app.platform.bunny.layer.IconButtonModel
import software.amazon.app.platform.bunny.layer.ItineraryButtonModel
import software.amazon.app.platform.bunny.layer.ItineraryContainerModel
import software.amazon.app.platform.bunny.layer.ModalModel
import software.amazon.app.platform.bunny.layer.NotificationModel
import software.amazon.app.platform.bunny.layer.PolaroidModel
import software.amazon.app.platform.bunny.layer.RearCameraModel
import software.amazon.app.platform.bunny.layer.RecenterButtonModel
import software.amazon.app.platform.bunny.layer.SpeedLimitModel
import software.amazon.app.platform.bunny.layer.TurnByTurnModel
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.renderer.ComposeRenderer
import software.amazon.app.platform.renderer.RendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer

sealed interface BunnyTemplate : BaseModel, AnimationContentKey {

  val baseContainer: BaseContainerModel
  val rearCamera: RearCameraModel?

  data class Welcome(
    override val baseContainer: BaseContainerModel,
    override val rearCamera: RearCameraModel?,
    val iconButton: IconButtonModel,
    val aboutMenuModel: AboutMenuModel,
    val modal: ModalModel,
  ) : BunnyTemplate {
    override val contentKey: Int by lazy {
      var result = this::class.hashCode()
      result = 31 * result + baseContainer.contentKey
      result = 31 * result + iconButton.contentKey
      result = 31 * result + aboutMenuModel.contentKey
      result = 31 * result + modal.contentKey
      result
    }
  }

  data class PreTravel(
    override val baseContainer: BaseContainerModel,
    override val rearCamera: RearCameraModel?,
    val iconButton: IconButtonModel,
    val itineraryContainer: ItineraryContainerModel,
    val recenterButton: RecenterButtonModel,
    val actionButton: ActionButtonModel,
    val aboutMenu: AboutMenuModel,
    val notification: NotificationModel,
    val modal: ModalModel,
  ) : BunnyTemplate {
    override val contentKey: Int by lazy {
      var result = this::class.hashCode()
      result = 31 * result + baseContainer.contentKey
      result = 31 * result + iconButton.contentKey
      result = 31 * result + itineraryContainer.contentKey
      result = 31 * result + recenterButton.contentKey
      result = 31 * result + actionButton.contentKey
      result = 31 * result + aboutMenu.contentKey
      result = 31 * result + notification.contentKey
      result = 31 * result + modal.contentKey
      result
    }
  }

  data class Driving(
    override val baseContainer: BaseContainerModel,
    override val rearCamera: RearCameraModel?,
    val iconButton: IconButtonModel,
    val itineraryContainer: ItineraryContainerModel,
    val recenterButton: RecenterButtonModel,
    val speedLimit: SpeedLimitModel,
    val turnByTurn: TurnByTurnModel,
    val eta: EtaModel,
    val aboutMenu: AboutMenuModel,
    val notification: NotificationModel,
    val modal: ModalModel,
  ) : BunnyTemplate {
    override val contentKey: Int by lazy {
      var result = this::class.hashCode()
      result = 31 * result + baseContainer.contentKey
      result = 31 * result + iconButton.contentKey
      result = 31 * result + itineraryContainer.contentKey
      result = 31 * result + recenterButton.contentKey
      result = 31 * result + speedLimit.contentKey
      result = 31 * result + turnByTurn.contentKey
      result = 31 * result + eta.contentKey
      result = 31 * result + aboutMenu.contentKey
      result = 31 * result + notification.contentKey
      result = 31 * result + modal.contentKey
      result
    }
  }

  data class Arriving(
    override val baseContainer: BaseContainerModel,
    override val rearCamera: RearCameraModel?,
    val iconButton: IconButtonModel,
    val recenterButton: RecenterButtonModel,
    val speedLimit: SpeedLimitModel,
    val turnByTurn: TurnByTurnModel,
    val eta: EtaModel,
    val polaroid: PolaroidModel,
    val itineraryContainer: ItineraryContainerModel,
    val itineraryButton: ItineraryButtonModel,
    val actionButton: ActionButtonModel,
    val aboutMenu: AboutMenuModel,
    val notification: NotificationModel,
    val modal: ModalModel,
  ) : BunnyTemplate {
    override val contentKey: Int by lazy {
      var result = this::class.hashCode()
      result = 31 * result + baseContainer.contentKey
      result = 31 * result + iconButton.contentKey
      result = 31 * result + recenterButton.contentKey
      result = 31 * result + speedLimit.contentKey
      result = 31 * result + turnByTurn.contentKey
      result = 31 * result + eta.contentKey
      result = 31 * result + polaroid.contentKey
      result = 31 * result + itineraryContainer.contentKey
      result = 31 * result + aboutMenu.contentKey
      result = 31 * result + notification.contentKey
      result = 31 * result + modal.contentKey
      result
    }
  }
}

@Inject
@ContributesRenderer
class BunnyTemplateRenderer(private val rendererFactory: RendererFactory) :
  ComposeRenderer<BunnyTemplate>() {
  @Composable
  override fun Compose(model: BunnyTemplate) {
    rendererFactory.getComposeRenderer(model.baseContainer).renderCompose(model.baseContainer)

    SharedTransitionLayout {
      CompositionLocalProvider(LocalSharedTransitionScope provides this) {
        AnimatedContent(
          targetState = model to LocalWindowSize.current,
          label = "Top level AnimatedContent",
          contentKey = { (template, windowSize) ->
            // Use the key from AnimationContentKey as indicator when content has changed
            // that needs to be animated. If this key is doesn't change (the default behavior),
            // then no animation occurs.
            template.contentKey + windowSize.ordinal
          },
        ) { (template, _) ->
          CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
            when (template) {
              is BunnyTemplate.Welcome -> BunnyTemplateWelcome(template)
              is BunnyTemplate.PreTravel -> BunnyTemplatePreTravel(template)
              is BunnyTemplate.Driving -> BunnyTemplateDriving(template)
              is BunnyTemplate.Arriving -> BunnyTemplateArriving(template)
            }
          }
        }
      }
    }

    // For demo purposes render the rear camera on top of everything else.
    val rearCamera = model.rearCamera
    if (rearCamera != null) {
      rendererFactory.getComposeRenderer(rearCamera).renderCompose(rearCamera)
    }
  }

  @Composable
  private fun BunnyTemplateWelcome(model: BunnyTemplate.Welcome) {
    when (LocalWindowSize.current) {
      WindowSize.Compact,
      WindowSize.Medium -> BunnyTemplateWelcomeCompact(model)
      WindowSize.Portrait -> BunnyTemplateWelcomePortrait(model)
      WindowSize.Large -> BunnyTemplateWelcomeLarge(model)
    }
  }

  @Composable
  private fun BunnyTemplateWelcomeCompact(model: BunnyTemplate.Welcome) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainer(
          left = {},
          right = {
            AnimatedVisibility(
              visible = model.aboutMenuModel.visible,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              rendererFactory
                .getComposeRenderer(model.aboutMenuModel)
                .renderCompose(model.aboutMenuModel)
            }
            AnimatedVisibility(
              visible = !model.aboutMenuModel.visible,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.TopEnd),
            ) {
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }
          },
        )
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateWelcomePortrait(model: BunnyTemplate.Welcome) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainer(
          rightPercentage = 0.75f,
          left = {},
          right = {
            AnimatedVisibility(
              visible = model.aboutMenuModel.visible,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              rendererFactory
                .getComposeRenderer(model.aboutMenuModel)
                .renderCompose(model.aboutMenuModel)
            }
            AnimatedVisibility(
              visible = !model.aboutMenuModel.visible,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.TopEnd),
            ) {
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }
          },
        )
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateWelcomeLarge(model: BunnyTemplate.Welcome) {
    Box(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
          if (model.rearCamera != null) {
            rendererFactory.getComposeRenderer(model.rearCamera).renderCompose(model.rearCamera)
          }
          Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            //          rendererFactory
            //            .getComposeRenderer(model.itineraryContainer)
            //            .renderCompose(model.itineraryContainer)
          }
        }
        PaddedFullScreen(modifier = Modifier.weight(1f)) {
          SplitContainer(
            left = {},
            right = {
              this@Row.AnimatedVisibility(
                visible = model.aboutMenuModel.visible,
                enter = fadeIn(),
                exit = fadeOut(),
              ) {
                rendererFactory
                  .getComposeRenderer(model.aboutMenuModel)
                  .renderCompose(model.aboutMenuModel)
              }
              this@Row.AnimatedVisibility(
                visible = !model.aboutMenuModel.visible,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopEnd),
              ) {
                rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
              }
            },
          )

          // Only the Interactive modal is rendered in this layer. The Fullscreen modal is rendered
          // in another layer on top.
          if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.Interactive) {
            Box(modifier = Modifier.align(Alignment.Center)) {
              rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
            }
          }
        }
      }

      // Only the Fullscreen modal is rendered in this layer. The Interactive modal is rendered
      // in another child layer.
      if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.FullScreen) {
        Box(modifier = Modifier.align(Alignment.Center)) {
          rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
        }
      }
    }
  }

  @Composable
  private fun BunnyTemplatePreTravel(model: BunnyTemplate.PreTravel) {
    when (LocalWindowSize.current) {
      WindowSize.Compact,
      WindowSize.Medium -> BunnyTemplatePreTravelCompact(model)
      WindowSize.Portrait -> BunnyTemplatePreTravelPortrait(model)
      WindowSize.Large -> BunnyTemplatePreTravelLarge(model)
    }
  }

  @Composable
  private fun BunnyTemplatePreTravelCompact(model: BunnyTemplate.PreTravel) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainer(
          left = {
            rendererFactory
              .getComposeRenderer(model.itineraryContainer)
              .renderCompose(model.itineraryContainer)
          },
          right = {
            Box(modifier = Modifier.fillMaxSize()) {
              Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
                rendererFactory
                  .getComposeRenderer(model.recenterButton)
                  .renderCompose(model.recenterButton)
                Spacer(modifier = Modifier.height(16.dp))
                rendererFactory
                  .getComposeRenderer(model.actionButton)
                  .renderCompose(model.actionButton)
              }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }
          },
        )

        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(
                fraction = if (LocalWindowSize.current == WindowSize.Compact) 0.75f else 0.5f
              )
              .align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.height(460.dp).fillMaxWidth(fraction = 0.625f).align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplatePreTravelPortrait(model: BunnyTemplate.PreTravel) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainerVertical(
          top = {
            Box(modifier = Modifier.fillMaxSize()) {
              Column(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
                rendererFactory
                  .getComposeRenderer(model.recenterButton)
                  .renderCompose(model.recenterButton)
                Spacer(modifier = Modifier.height(16.dp))
                rendererFactory
                  .getComposeRenderer(model.actionButton)
                  .renderCompose(model.actionButton)
              }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }
          },
          bottom = {
            rendererFactory
              .getComposeRenderer(model.itineraryContainer)
              .renderCompose(model.itineraryContainer)
          },
        )

        Box(
          modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.75f).align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.fillMaxHeight(fraction = 0.375f).fillMaxWidth().align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplatePreTravelLarge(model: BunnyTemplate.PreTravel) {
    Box(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
          if (model.rearCamera != null) {
            rendererFactory.getComposeRenderer(model.rearCamera).renderCompose(model.rearCamera)
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 32.dp, top = 32.dp, bottom = 32.dp)
          ) {
            rendererFactory
              .getComposeRenderer(model.itineraryContainer)
              .renderCompose(model.itineraryContainer)
          }
        }
        PaddedFullScreen(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Box(modifier = Modifier.align(Alignment.BottomStart)) {
            rendererFactory
              .getComposeRenderer(model.recenterButton)
              .renderCompose(model.recenterButton)
          }
          Row(modifier = Modifier.align(Alignment.BottomCenter)) {
            Spacer(Modifier.weight(1f))
            Box(Modifier.weight(2f)) {
              rendererFactory
                .getComposeRenderer(model.actionButton)
                .renderCompose(model.actionButton)
            }
            Spacer(Modifier.weight(1f))
          }
          Box(modifier = Modifier.align(Alignment.TopEnd)) {
            rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
          }
          Box(
            modifier =
              Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.5f).align(Alignment.TopEnd)
          ) {
            rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
          }

          Box(
            modifier =
              Modifier.height(460.dp).fillMaxWidth(fraction = 0.625f).align(Alignment.TopStart)
          ) {
            rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
          }

          // Only the Interactive modal is rendered in this layer. The Fullscreen modal is rendered
          // in another layer on top.
          if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.Interactive) {
            Box(modifier = Modifier.align(Alignment.Center)) {
              rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
            }
          }
        }
      }

      // Only the Fullscreen modal is rendered in this layer. The Interactive modal is rendered
      // in another child layer.
      if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.FullScreen) {
        Box(modifier = Modifier.align(Alignment.Center)) {
          rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
        }
      }
    }
  }

  @Composable
  private fun BunnyTemplateDriving(model: BunnyTemplate.Driving) {
    when (LocalWindowSize.current) {
      WindowSize.Compact,
      WindowSize.Medium -> BunnyTemplateDrivingCompact(model)
      WindowSize.Portrait -> BunnyTemplateDrivingPortrait(model)
      WindowSize.Large -> BunnyTemplateDrivingLarge(model)
    }
  }

  @Composable
  private fun BunnyTemplateDrivingCompact(model: BunnyTemplate.Driving) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainer(
          left = {
            Row {
              Box(modifier = Modifier.weight(1f)) {
                rendererFactory.getComposeRenderer(model.turnByTurn).renderCompose(model.turnByTurn)
              }
              if (LocalWindowSize.current == WindowSize.Medium) {
                Spacer(Modifier.width(16.dp))
                rendererFactory.getComposeRenderer(model.speedLimit).renderCompose(model.speedLimit)
              }
            }
          },
          right = {
            if (LocalWindowSize.current == WindowSize.Compact) {
              Box(modifier = Modifier.align(Alignment.TopStart)) {
                rendererFactory.getComposeRenderer(model.speedLimit).renderCompose(model.speedLimit)
              }
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }
            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
              rendererFactory
                .getComposeRenderer(model.recenterButton)
                .renderCompose(model.recenterButton)
            }
          },
        )

        Box(modifier = Modifier.fillMaxWidth(fraction = 0.375f).align(Alignment.BottomCenter)) {
          rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
        }

        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(
                fraction = if (LocalWindowSize.current == WindowSize.Compact) 0.75f else 0.5f
              )
              .align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.height(460.dp)
              .fillMaxWidth(
                fraction = if (LocalWindowSize.current == WindowSize.Compact) 0.75f else 0.625f
              )
              .align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateDrivingPortrait(model: BunnyTemplate.Driving) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        Row {
          Column(modifier = Modifier.weight(1f)) {
            rendererFactory.getComposeRenderer(model.turnByTurn).renderCompose(model.turnByTurn)
            Spacer(Modifier.height(16.dp))
            rendererFactory.getComposeRenderer(model.speedLimit).renderCompose(model.speedLimit)
          }
          Spacer(Modifier.width(16.dp))
          rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
        }

        Box(modifier = Modifier.align(Alignment.BottomStart)) {
          rendererFactory
            .getComposeRenderer(model.recenterButton)
            .renderCompose(model.recenterButton)
        }

        Box(modifier = Modifier.fillMaxWidth(fraction = 0.625f).align(Alignment.BottomCenter)) {
          rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
        }

        Box(
          modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.75f).align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.fillMaxHeight(fraction = 0.375f).fillMaxWidth().align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateDrivingLarge(model: BunnyTemplate.Driving) {
    Box(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
          if (model.rearCamera != null) {
            rendererFactory.getComposeRenderer(model.rearCamera).renderCompose(model.rearCamera)
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 32.dp, top = 32.dp, bottom = 32.dp)
          ) {
            rendererFactory
              .getComposeRenderer(model.itineraryContainer)
              .renderCompose(model.itineraryContainer)
          }
        }
        PaddedFullScreen(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Box(modifier = Modifier.align(Alignment.BottomStart)) {
            rendererFactory
              .getComposeRenderer(model.recenterButton)
              .renderCompose(model.recenterButton)
          }

          Box(modifier = Modifier.fillMaxWidth(fraction = 0.375f).align(Alignment.BottomCenter)) {
            rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
          }

          Box(modifier = Modifier.align(Alignment.TopEnd)) {
            rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
          }

          Box(
            modifier =
              Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.5f).align(Alignment.TopEnd)
          ) {
            rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
          }

          Box(
            modifier =
              Modifier.height(460.dp).fillMaxWidth(fraction = 0.625f).align(Alignment.TopStart)
          ) {
            rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
          }

          // Only the Interactive modal is rendered in this layer. The Fullscreen modal is rendered
          // in another layer on top.
          if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.Interactive) {
            Box(modifier = Modifier.align(Alignment.Center)) {
              rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
            }
          }
        }
      }

      // Only the Fullscreen modal is rendered in this layer. The Interactive modal is rendered
      // in another child layer.
      if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.FullScreen) {
        Box(modifier = Modifier.align(Alignment.Center)) {
          rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
        }
      }
    }
  }

  @Composable
  private fun BunnyTemplateArriving(model: BunnyTemplate.Arriving) {
    when (LocalWindowSize.current) {
      WindowSize.Compact,
      WindowSize.Medium -> BunnyTemplateArrivingCompact(model)
      WindowSize.Portrait -> BunnyTemplateArrivingPortrait(model)
      WindowSize.Large -> BunnyTemplateArrivingLarge(model)
    }
  }

  @Composable
  private fun BunnyTemplateArrivingCompact(model: BunnyTemplate.Arriving) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        SplitContainer(
          left = {
            Row {
              Box(modifier = Modifier.weight(1f)) {
                rendererFactory.getComposeRenderer(model.turnByTurn).renderCompose(model.turnByTurn)
              }
              if (LocalWindowSize.current == WindowSize.Medium) {
                Spacer(Modifier.width(16.dp))
                rendererFactory.getComposeRenderer(model.speedLimit).renderCompose(model.speedLimit)
              }
            }

            Row(modifier = Modifier.fillMaxWidth().align(Alignment.BottomStart)) {
              Column(modifier = Modifier.weight(1f).align(Alignment.Bottom)) {
                rendererFactory
                  .getComposeRenderer(model.actionButton)
                  .renderCompose(model.actionButton)

                Spacer(Modifier.height(16.dp))

                Box(modifier = Modifier.widthIn(max = 400.dp)) {
                  rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
                }
              }

              Spacer(Modifier.width(16.dp))

              Column(modifier = Modifier.align(Alignment.Bottom)) {
                rendererFactory
                  .getComposeRenderer(model.itineraryButton)
                  .renderCompose(model.itineraryButton)

                Spacer(Modifier.height(16.dp))

                rendererFactory
                  .getComposeRenderer(model.recenterButton)
                  .renderCompose(model.recenterButton)
              }
            }
          },
          right = {
            Column {
              Row {
                if (LocalWindowSize.current == WindowSize.Compact) {
                  rendererFactory
                    .getComposeRenderer(model.speedLimit)
                    .renderCompose(model.speedLimit)
                }
                Spacer(modifier = Modifier.weight(1f))
                Box(modifier = Modifier) {
                  rendererFactory
                    .getComposeRenderer(model.iconButton)
                    .renderCompose(model.iconButton)
                }
              }

              Box(modifier = Modifier.fillMaxSize().weight(1f).padding(top = 16.dp)) {
                this@Column.AnimatedVisibility(
                  visible = model.itineraryContainer.visible,
                  enter = fadeIn(),
                  exit = fadeOut(),
                ) {
                  rendererFactory
                    .getComposeRenderer(model.itineraryContainer)
                    .renderCompose(model.itineraryContainer)
                }

                this@Column.AnimatedVisibility(
                  visible = !model.itineraryContainer.visible,
                  enter = fadeIn(),
                  exit = fadeOut(),
                  modifier = Modifier.align(Alignment.BottomEnd),
                ) {
                  rendererFactory.getComposeRenderer(model.polaroid).renderCompose(model.polaroid)
                }
              }
            }
          },
        )

        Box(
          modifier =
            Modifier.fillMaxHeight()
              .fillMaxWidth(
                fraction = if (LocalWindowSize.current == WindowSize.Compact) 0.75f else 0.5f
              )
              .align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.height(460.dp)
              .fillMaxWidth(
                fraction = if (LocalWindowSize.current == WindowSize.Compact) 0.75f else 0.625f
              )
              .align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateArrivingPortrait(model: BunnyTemplate.Arriving) {
    Box(modifier = Modifier.fillMaxSize()) {
      PaddedFullScreen {
        Column(modifier = Modifier.fillMaxSize()) {
          val height by animateFloatAsState(if (model.itineraryContainer.visible) 0.6f else 0.75f)

          Box(modifier = Modifier.fillMaxHeight(height).padding(bottom = 24.dp)) {
            Row {
              Column(modifier = Modifier.weight(1f)) {
                rendererFactory.getComposeRenderer(model.turnByTurn).renderCompose(model.turnByTurn)
                Spacer(Modifier.height(16.dp))
                rendererFactory.getComposeRenderer(model.speedLimit).renderCompose(model.speedLimit)
              }
              Spacer(Modifier.width(16.dp))
              rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
            }

            Column(modifier = Modifier.align(Alignment.BottomStart)) {
              rendererFactory
                .getComposeRenderer(model.itineraryButton)
                .renderCompose(model.itineraryButton)

              Spacer(modifier = Modifier.height(16.dp))

              rendererFactory
                .getComposeRenderer(model.recenterButton)
                .renderCompose(model.recenterButton)
            }

            Box(modifier = Modifier.fillMaxWidth(fraction = 0.625f).align(Alignment.BottomCenter)) {
              rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
            }

            Box(modifier = Modifier.align(Alignment.BottomEnd)) {
              rendererFactory
                .getComposeRenderer(model.actionButton)
                .renderCompose(model.actionButton)
            }
          }

          Box(modifier = Modifier.align(Alignment.CenterHorizontally)) {
            this@Column.AnimatedVisibility(
              visible = model.itineraryContainer.visible,
              enter = fadeIn(),
              exit = fadeOut(),
            ) {
              rendererFactory
                .getComposeRenderer(model.itineraryContainer)
                .renderCompose(model.itineraryContainer)
            }

            this@Column.AnimatedVisibility(
              visible = !model.itineraryContainer.visible,
              enter = fadeIn(),
              exit = fadeOut(),
              modifier = Modifier.align(Alignment.BottomEnd),
            ) {
              rendererFactory.getComposeRenderer(model.polaroid).renderCompose(model.polaroid)
            }
          }
        }

        Box(
          modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.75f).align(Alignment.TopEnd)
        ) {
          rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
        }

        Box(
          modifier =
            Modifier.fillMaxHeight(fraction = 0.375f).fillMaxWidth().align(Alignment.TopStart)
        ) {
          rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
        }
      }

      Box(modifier = Modifier.align(Alignment.Center)) {
        rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
      }
    }
  }

  @Composable
  private fun BunnyTemplateArrivingLarge(model: BunnyTemplate.Arriving) {
    Box(modifier = Modifier.fillMaxSize()) {
      Row(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxHeight().width(IntrinsicSize.Min)) {
          if (model.rearCamera != null) {
            rendererFactory.getComposeRenderer(model.rearCamera).renderCompose(model.rearCamera)
          }
          Box(
            modifier =
              Modifier.fillMaxWidth()
                .fillMaxHeight()
                .padding(start = 32.dp, top = 32.dp, bottom = 32.dp)
          ) {
            rendererFactory
              .getComposeRenderer(model.itineraryContainer)
              .renderCompose(model.itineraryContainer)
          }
        }
        PaddedFullScreen(modifier = Modifier.weight(1f).fillMaxHeight()) {
          Row(modifier = Modifier.align(Alignment.BottomStart).fillMaxWidth()) {
            rendererFactory
              .getComposeRenderer(model.recenterButton)
              .renderCompose(model.recenterButton)

            Spacer(modifier = Modifier.width(16.dp))
            Box(modifier = Modifier.widthIn(max = 400.dp)) {
              rendererFactory.getComposeRenderer(model.eta).renderCompose(model.eta)
            }
            Spacer(modifier = Modifier.width(16.dp))

            rendererFactory.getComposeRenderer(model.actionButton).renderCompose(model.actionButton)
          }

          Box(modifier = Modifier.align(Alignment.TopEnd)) {
            rendererFactory.getComposeRenderer(model.iconButton).renderCompose(model.iconButton)
          }

          Box(
            modifier =
              Modifier.fillMaxHeight().fillMaxWidth(fraction = 0.5f).align(Alignment.TopEnd)
          ) {
            rendererFactory.getComposeRenderer(model.aboutMenu).renderCompose(model.aboutMenu)
          }

          Box(modifier = Modifier.align(Alignment.TopStart)) {
            rendererFactory.getComposeRenderer(model.polaroid).renderCompose(model.polaroid)
          }

          Box(
            modifier =
              Modifier.height(460.dp).fillMaxWidth(fraction = 0.625f).align(Alignment.TopStart)
          ) {
            rendererFactory.getComposeRenderer(model.notification).renderCompose(model.notification)
          }

          // Only the Interactive modal is rendered in this layer. The Fullscreen modal is rendered
          // in another layer on top.
          if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.Interactive) {
            Box(modifier = Modifier.align(Alignment.Center)) {
              rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
            }
          }
        }
      }

      // Only the Fullscreen modal is rendered in this layer. The Interactive modal is rendered
      // in another child layer.
      if (model.modal is ModalModel.Hide || model.modal is ModalModel.Show.FullScreen) {
        Box(modifier = Modifier.align(Alignment.Center)) {
          rendererFactory.getComposeRenderer(model.modal).renderCompose(model.modal)
        }
      }
    }
  }

  @Composable
  private fun PaddedFullScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
  ) {
    Box(modifier = modifier.padding(32.dp)) { content() }
  }

  @Composable
  private fun SplitContainer(
    left: @Composable BoxScope.() -> Unit,
    right: @Composable BoxScope.() -> Unit,
    rightPercentage: Float = 0.5f,
  ) {
    Row(modifier = Modifier.fillMaxSize()) {
      Box(modifier = Modifier.weight(1f - rightPercentage).fillMaxHeight().padding(end = 16.dp)) {
        left()
      }
      Box(modifier = Modifier.weight(rightPercentage).fillMaxHeight().padding(start = 16.dp)) {
        right()
      }
    }
  }

  @Composable
  private fun SplitContainerVertical(
    top: @Composable BoxScope.() -> Unit,
    bottom: @Composable BoxScope.() -> Unit,
    bottomPercentage: Float = 0.375f,
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier.weight(1f - bottomPercentage).fillMaxWidth().padding(bottom = 16.dp)
      ) {
        top()
      }
      Box(modifier = Modifier.weight(bottomPercentage).fillMaxWidth().padding(top = 16.dp)) {
        bottom()
      }
    }
  }
}
