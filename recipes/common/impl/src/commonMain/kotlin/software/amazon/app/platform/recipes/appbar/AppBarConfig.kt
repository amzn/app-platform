package software.amazon.app.platform.recipes.appbar

/** Configures the look of the App Bar for the recipe app. */
data class AppBarConfig(
  /** The title shown in the center of the App Bar. */
  val title: String,
  /**
   * If not null, then the back arrow will be shown and the lambda invoked when there's an action.
   */
  val backArrowAction: (() -> Unit)? = null,
) {
  companion object {
    /** The default configuration used when no presenter overrides the config. */
    val DEFAULT = AppBarConfig(title = "Recipes App")
  }
}
