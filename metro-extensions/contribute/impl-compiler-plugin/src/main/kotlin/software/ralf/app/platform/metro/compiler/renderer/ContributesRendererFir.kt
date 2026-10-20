package software.ralf.app.platform.metro.compiler.renderer

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension
import dev.zacsweers.metro.compiler.api.fir.MetroContributionHintExtension.ContributionHint
import dev.zacsweers.metro.compiler.api.fir.MetroFirDeclarationGenerationExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys
import software.ralf.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.ralf.app.platform.metro.compiler.fir.buildClassExpression
import software.ralf.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation

public class ContributesRendererFir
internal constructor(
  session: FirSession,
  private val compatContext: CompatContext,
) : MetroFirDeclarationGenerationExtension(session), MetroContributionHintExtension {
  public constructor(session: FirSession) : this(session, CompatContext.create())

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRendererIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRendererClasses().map { classSymbol ->
      ContributionHint(
        contributingClassId =
          classSymbol.classId.createNestedClassId(
            ContributesRendererIds.NESTED_INTERFACE_NAME,
          ),
        scope = ClassIds.RENDERER_SCOPE,
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    generatedRendererContributionOwner(classSymbol)?.let { rendererOwner ->
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptySet()
      return if (!metadata.hasInjectAnnotation || metadata.modelClasses.isNotEmpty()) {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      } else {
        emptySet()
      }
    }

    return if (
      hasAnnotation(
        classSymbol,
        ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID,
        session,
      )
    ) {
      setOf(ContributesRendererIds.NESTED_INTERFACE_NAME)
    } else {
      emptySet()
    }
  }

  override fun generateNestedClassLikeDeclaration(
    owner: FirClassSymbol<*>,
    name: Name,
    context: NestedClassGenerationContext,
  ): FirClassLikeSymbol<*>? {
    if (name == SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      val rendererOwner = generatedRendererContributionOwner(owner) ?: return null
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return null
      if (metadata.hasInjectAnnotation && metadata.modelClasses.isEmpty()) {
        return null
      }
      val contributionSymbol = owner as? FirRegularClassSymbol ?: return null
      return createCompanionObject(contributionSymbol, Keys.ContributesRendererGeneratorKey).symbol
    }

    if (name != ContributesRendererIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session))
      return null

    val rendererOwner = owner as? FirRegularClassSymbol ?: return null
    rendererContributionMetadata(rendererOwner, session) ?: return null
    val nestedClassId = rendererOwner.classId.createNestedClassId(name)
    val graphSymbol = FirRegularClassSymbol(nestedClassId)

    val contributionClass = buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = rendererOwner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = graphSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(rendererOwner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations += buildSimpleAnnotationCall(ClassIds.BINDING_CONTAINER, graphSymbol, session)
      annotations += buildReflectionContributesToAnnotation()
      annotations += buildOriginAnnotation(rendererOwner, graphSymbol)
    }

    return contributionClass.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    return if (generatedRendererProviderCompanionOwner(classSymbol) != null) {
      setOf(SpecialNames.INIT)
    } else {
      emptySet()
    }
  }

  override fun generateConstructors(
    context: MemberGenerationContext,
  ): List<FirConstructorSymbol> {
    if (generatedRendererProviderCompanionOwner(context.owner) == null) {
      return emptyList()
    }
    val constructor: FirConstructor =
      createDefaultPrivateConstructor(context.owner, Keys.ContributesRendererGeneratorKey)
    return listOf(constructor.symbol)
  }

  private fun generatedRendererContributionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != ContributesRendererIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = classSymbol.classId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
    }
  }

  private fun generatedRendererProviderCompanionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      return null
    }
    val contributionClassId = classSymbol.classId.outerClassId ?: return null
    if (contributionClassId.shortClassName != ContributesRendererIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = contributionClassId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    val metadata = rendererContributionMetadata(ownerSymbol, session) ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session) &&
        (!metadata.hasInjectAnnotation || metadata.modelClasses.isNotEmpty())
    }
  }

  private fun annotatedRendererClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRendererIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildReflectionContributesToAnnotation() = buildAnnotation {
    val contributesToSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.CONTRIBUTES_TO)
        as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.CONTRIBUTES_TO} not found on the classpath")
    annotationTypeRef = contributesToSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("scope")] =
        buildClassExpression(ClassIds.RENDERER_SCOPE, session, compatContext)
    }
  }

  private fun buildOriginAnnotation(
    owner: FirRegularClassSymbol,
    containingSymbol: FirRegularClassSymbol,
  ) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.ORIGIN,
      argName = Name.identifier("value"),
      argument = buildClassExpression(owner, session, compatContext),
      containingSymbol = containingSymbol,
      session = session,
    )

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRendererFir(session, compatContext)
  }
}
