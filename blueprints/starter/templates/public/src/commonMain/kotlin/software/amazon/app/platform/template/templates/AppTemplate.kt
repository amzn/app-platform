package software.amazon.app.platform.template.templates

import software.amazon.app.platform.presenter.BaseModel
import software.amazon.app.platform.presenter.template.Template

/** All [Template]s implemented in the sample application. */
sealed interface AppTemplate : Template {
    /** A template that hosts a single model, which should rendered as full-screen element. */
    data class FullScreenTemplate(
        /** The model to be rendered fullscreen. */
        val model: BaseModel,
    ) : AppTemplate

    data class HeaderDetailTemplate(
        val header: BaseModel,
        val detail: BaseModel,
    ) : AppTemplate
}
