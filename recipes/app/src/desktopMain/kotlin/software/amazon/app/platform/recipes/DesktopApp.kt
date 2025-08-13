package software.amazon.app.platform.recipes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.key
import kotlinx.coroutines.flow.MutableSharedFlow
import software.amazon.app.platform.bunny.BunnyTemplate
import software.amazon.app.platform.bunny.LocalWindowSize
import software.amazon.app.platform.bunny.WindowSize
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
import software.amazon.app.platform.renderer.ComposeRendererFactory
import software.amazon.app.platform.renderer.getComposeRenderer
import software.amazon.app.platform.scope.RootScopeProvider
import software.amazon.app.platform.scope.Scope
import software.amazon.app.platform.scope.di.diComponent
import software.amazon.lastmile.kotlin.inject.anvil.AppScope
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo

/**
 * Responsible for creating the app component [component] and producing templates. Call [destroy] to
 * clean up any resources.
 */
class DesktopApp(private val component: (RootScopeProvider) -> AppComponent) : RootScopeProvider {

  override val rootScope: Scope
    get() = demoApplication.rootScope

  private val demoApplication = DemoApplication().apply { create(component(this)) }

  private val templateProvider =
    rootScope.diComponent<Component>().templateProviderFactory.createTemplateProvider()

  /** Call this composable function to start rendering templates on the screen. */
  @Composable
  fun renderTemplates(keyEventFlow: MutableSharedFlow<KeyEvent>) {
    val factory = remember { ComposeRendererFactory(demoApplication) }

    val templateClasses =
      listOf(
        BunnyTemplate.Welcome::class,
        BunnyTemplate.PreTravel::class,
        BunnyTemplate.Driving::class,
        BunnyTemplate.Arriving::class,
      )
    var templateClassIndex by remember { mutableIntStateOf(0) }

    val largeScreen = LocalWindowSize.current == WindowSize.Large
    val portrait = LocalWindowSize.current == WindowSize.Portrait

    val rearCamera = if (largeScreen) RearCameraModel else null

    val preTravelActionButtonMaxWidth = portrait
    val drivingEtaShowAddress = !largeScreen
    val drivingItineraryContainer =
      if (largeScreen) ItineraryContainerModel.Show else ItineraryContainerModel.Hide
    val arrivingItineraryContainer =
      if (largeScreen) ItineraryContainerModel.Show else ItineraryContainerModel.Hide
    val arrivingEtaShowAddress = !largeScreen
    val arrivingShowActionButtonAlways = largeScreen
    val arrivingActionButtonHorizontal = !portrait

    val templates =
      remember(templateClassIndex, rearCamera, LocalWindowSize.current) {
        listOfNotNull(
            BunnyTemplate.Welcome(
              baseContainer = BaseContainerModel("Welcome"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(),
              aboutMenuModel = AboutMenuModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Welcome(
              baseContainer = BaseContainerModel("Welcome"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(),
              aboutMenuModel = AboutMenuModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Welcome(
              baseContainer = BaseContainerModel("Welcome"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(),
              aboutMenuModel = AboutMenuModel.Show(),
              modal = ModalModel.Show.FullScreen(),
            ),
            BunnyTemplate.Welcome(
              baseContainer = BaseContainerModel("Welcome"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(),
              aboutMenuModel = AboutMenuModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Welcome(
              baseContainer = BaseContainerModel("Welcome"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(),
              aboutMenuModel = AboutMenuModel.Hide,
              modal = ModalModel.Show.Interactive(),
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Show(),
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Show(),
              notification = NotificationModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.PreTravel(
              baseContainer = BaseContainerModel("Pre Travel"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 4),
              itineraryContainer = ItineraryContainerModel.Show,
              recenterButton = RecenterButtonModel(),
              actionButton = ActionButtonModel.Show(applyMaxWidth = preTravelActionButtonMaxWidth),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Show.FullScreen(),
            ),
            BunnyTemplate.Driving(
              baseContainer = BaseContainerModel("Driving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              itineraryContainer = drivingItineraryContainer,
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = true),
              eta = EtaModel(showAddress = drivingEtaShowAddress, showTime = true),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Driving(
              baseContainer = BaseContainerModel("Driving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              itineraryContainer = drivingItineraryContainer,
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = true),
              eta = EtaModel(showAddress = drivingEtaShowAddress, showTime = true),
              aboutMenu = AboutMenuModel.Show(),
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Driving(
              baseContainer = BaseContainerModel("Driving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              itineraryContainer = drivingItineraryContainer,
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = true),
              eta = EtaModel(showAddress = drivingEtaShowAddress, showTime = true),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Driving(
              baseContainer = BaseContainerModel("Driving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              itineraryContainer = drivingItineraryContainer,
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = true),
              eta = EtaModel(showAddress = drivingEtaShowAddress, showTime = true),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Show.FullScreen(),
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = arrivingItineraryContainer,
              itineraryButton = ItineraryButtonModel.Hide,
              actionButton =
                if (arrivingShowActionButtonAlways) ActionButtonModel.Show(applyMaxWidth = true)
                else ActionButtonModel.Hide,
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Hide,
              actionButton =
                if (arrivingShowActionButtonAlways) ActionButtonModel.Show(applyMaxWidth = true)
                else ActionButtonModel.Hide,
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Show,
              actionButton =
                ActionButtonModel.Show(
                  applyMaxWidth = true,
                  horizontal = arrivingActionButtonHorizontal,
                ),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = largeScreen),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Show,
              actionButton =
                ActionButtonModel.Show(
                  applyMaxWidth = true,
                  horizontal = arrivingActionButtonHorizontal,
                ),
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Hide,
              actionButton =
                if (arrivingShowActionButtonAlways) ActionButtonModel.Show(applyMaxWidth = true)
                else ActionButtonModel.Hide,
              aboutMenu = AboutMenuModel.Show(),
              notification = NotificationModel.Hide,
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Hide,
              actionButton =
                if (arrivingShowActionButtonAlways) ActionButtonModel.Show(applyMaxWidth = true)
                else ActionButtonModel.Hide,
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Show(),
              modal = ModalModel.Hide,
            ),
            BunnyTemplate.Arriving(
              baseContainer = BaseContainerModel("Arriving"),
              rearCamera = rearCamera,
              iconButton = IconButtonModel(count = 1),
              recenterButton = RecenterButtonModel(),
              speedLimit = SpeedLimitModel(),
              turnByTurn = TurnByTurnModel(showAddress = false),
              eta = EtaModel(showAddress = arrivingEtaShowAddress, showTime = true),
              polaroid = PolaroidModel(),
              itineraryContainer = ItineraryContainerModel.Show,
              itineraryButton = ItineraryButtonModel.Hide,
              actionButton =
                if (arrivingShowActionButtonAlways) ActionButtonModel.Show(applyMaxWidth = true)
                else ActionButtonModel.Hide,
              aboutMenu = AboutMenuModel.Hide,
              notification = NotificationModel.Hide,
              modal = ModalModel.Show.FullScreen(),
            ),
          )
          .filter { templateClasses[templateClassIndex].isInstance(it) }
      }

    var templateIndex by remember { mutableIntStateOf(0) }

    LaunchedEffect(templateClassIndex) {
      keyEventFlow.collect {
        when (it.key) {
          Key.DirectionLeft -> {
            templateIndex = (templateIndex - 1 + templates.size).mod(templates.size)
          }
          Key.DirectionRight -> {
            templateIndex = (templateIndex + 1).mod(templates.size)
          }
          Key.DirectionDown -> {
            templateIndex = 0
            templateClassIndex =
              (templateClassIndex - 1 + templateClasses.size).mod(templateClasses.size)
          }
          Key.DirectionUp -> {
            templateIndex = 0
            templateClassIndex = (templateClassIndex + 1).mod(templateClasses.size)
          }
        }
      }
    }

    val template = templates[templateIndex]
    factory.getComposeRenderer(template).renderCompose(template)
  }

  /** Cancels and releases all resources. */
  fun destroy() {
    templateProvider.cancel()
    demoApplication.destroy()
  }

  /** Component interface to give us access to objects from the app component. */
  @ContributesTo(AppScope::class)
  interface Component {
    /** Gives access to the [TemplateProvider.Factory] from the object graph. */
    val templateProviderFactory: TemplateProvider.Factory
  }
}
