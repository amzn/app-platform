package software.amazon.app.platform.recipes.appbar

import software.amazon.app.platform.presenter.BaseModel

/** Can be implemented by a [BaseModel] class to change the configuration of the App Bar. */
interface AppBarConfigModel {
  /** Returns the config that should be rendered. */
  fun appBarConfig(): AppBarConfig
}
