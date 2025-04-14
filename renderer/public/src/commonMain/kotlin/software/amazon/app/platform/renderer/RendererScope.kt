package software.amazon.app.platform.renderer

import software.amazon.app.platform.inject.ContributesRenderer

/**
 * A Scope specifically for `Renderer` implementations.
 *
 * Renderers annotated with the [ContributesRenderer] annotation have a kotlin-inject component
 * generated with all necessary bindings.
 *
 * This scope should not be contributed to directly, instead prefer [ContributesRenderer]. See
 * [ContributesRenderer] for more details.
 */
public abstract class RendererScope private constructor()
