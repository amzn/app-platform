package software.amazon.app.platform.metro.compiler.renderer

import com.google.auto.service.AutoService
import dev.zacsweers.metro.compiler.MetroOptions
import dev.zacsweers.metro.compiler.api.fir.MetroFirDeclarationGenerationExtension
import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.ConeClassLikeLookupTagImpl
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.Keys
import software.amazon.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.amazon.app.platform.metro.compiler.fir.buildClassExpression
import software.amazon.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.amazon.app.platform.metro.compiler.fir.hasAnnotation

/**
 * Generates the declaration shape for `@ContributesRenderer` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model> {
 *
 *   @ContributesTo(RendererScope::class)
 *   @Origin(TestRenderer::class)
 *   interface RendererContribution {
 *     @Provides
 *     fun provideTestRenderer(): TestRenderer
 *
 *     @Binds
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererModel(renderer: TestRenderer): Renderer<*>
 *
 *     @Provides
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     @ForScope(RendererScope::class)
 *     fun provideTestRendererModelKey(): KClass<out Renderer<*>>
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the renderer has an `@Inject` constructor, the
 * nested declaration omits `provideTestRenderer()` and only the map bindings are generated. When
 * `includeSealedSubtypes` is enabled, the `Model` binding pair is generated once per collected
 * model subtype.
 */
public class ContributesRendererFir(session: FirSession) :
  MetroFirDeclarationGenerationExtension(session) {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRendererIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRendererClasses().map { classSymbol ->
      ContributionHint(
        contributingClassId =
          classSymbol.classId.createNestedClassId(ContributesRendererIds.NESTED_INTERFACE_NAME),
        scope = ClassIds.RENDERER_SCOPE,
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    return if (
      hasAnnotation(classSymbol, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session)
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
    if (name != ContributesRendererIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesRendererIds.CONTRIBUTES_RENDERER_CLASS_ID, session))
      return null

    val rendererOwner = owner as? FirRegularClassSymbol ?: return null
    val metadata = rendererContributionMetadata(rendererOwner, session) ?: return null
    val nestedClassId = rendererOwner.classId.createNestedClassId(name)
    val graphSymbol = FirRegularClassSymbol(nestedClassId)

    buildRegularClass {
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
      annotations += buildReflectionContributesToAnnotation()
      annotations += buildOriginAnnotation(rendererOwner, graphSymbol)
      for (function in buildProvidesFunctions(nestedClassId, rendererOwner, metadata)) {
        declarations += function
      }
    }

    return graphSymbol
  }

  private fun annotatedRendererClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRendererIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildProvidesFunctions(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    metadata: RendererContributionMetadata,
    includeRendererProvider: Boolean = true,
  ): List<FirFunction> {
    val functions = mutableListOf<FirFunction>()
    if (includeRendererProvider && !metadata.hasInjectAnnotation) {
      functions += buildProvideRendererFunction(graphClassId, owner)
    }
    metadata.modelClasses.forEach { modelClass ->
      functions += buildProvideRendererIntoMapFunction(graphClassId, owner, modelClass)
      functions += buildProvideRendererKeyFunction(graphClassId, owner, modelClass)
    }
    return functions
  }

  private fun buildProvideRendererFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
    }
  }

  private fun buildProvideRendererIntoMapFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}" +
        ContributesRendererIds.generatedModelClassNameSuffix(modelClass.classId)
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val ownerType = owner.defaultType()

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = rendererStarType().toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      valueParameters += buildValueParameter {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesRendererGeneratorKey.origin
        source = owner.source
        returnTypeRef = ownerType.toFirResolvedTypeRef()
        name = Name.identifier("renderer")
        symbol = FirValueParameterSymbol()
        containingDeclarationSymbol = functionSymbol
      }
      annotations += buildSimpleAnnotationCall(ClassIds.BINDS, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRendererKeyAnnotation(owner, modelClass, functionSymbol)
    }
  }

  private fun buildProvideRendererKeyFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): FirFunction {
    val functionName =
      "provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}" +
        ContributesRendererIds.generatedModelClassNameSuffix(modelClass.classId) +
        "Key"
    val callableId = CallableId(graphClassId, Name.identifier(functionName))
    val functionSymbol = FirNamedFunctionSymbol(callableId)

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = kClassProducerOf(rendererStarType()).toFirResolvedTypeRef()
      dispatchReceiverType = generatedGraphType(graphClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
      annotations += buildSimpleAnnotationCall(ClassIds.INTO_MAP, functionSymbol, session)
      annotations += buildRendererKeyAnnotation(owner, modelClass, functionSymbol)
      annotations += buildForScopeAnnotation(functionSymbol)
    }
  }

  private fun generatedGraphType(graphClassId: ClassId) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(graphClassId),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun rendererStarType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.RENDERER),
      arrayOf(ConeStarProjection),
      isMarkedNullable = false,
    )

  private fun kClassProducerOf(type: ConeKotlinType) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))),
      arrayOf(ConeKotlinTypeProjectionOut(type)),
      isMarkedNullable = false,
    )

  private fun buildReflectionContributesToAnnotation() = buildAnnotation {
    val contributesToSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.CONTRIBUTES_TO)
        as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.CONTRIBUTES_TO} not found on the classpath")
    annotationTypeRef = contributesToSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("scope")] = buildClassExpression(ClassIds.RENDERER_SCOPE, session)
    }
  }

  private fun buildForScopeAnnotation(containingSymbol: FirNamedFunctionSymbol) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.FOR_SCOPE,
      argName = Name.identifier("scope"),
      argument = buildClassExpression(ClassIds.RENDERER_SCOPE, session),
      containingSymbol = containingSymbol,
      session = session,
    )

  private fun buildOriginAnnotation(
    owner: FirRegularClassSymbol,
    containingSymbol: FirRegularClassSymbol,
  ) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.ORIGIN,
      argName = Name.identifier("value"),
      argument = buildClassExpression(owner, session),
      containingSymbol = containingSymbol,
      session = session,
    )

  private fun buildRendererKeyAnnotation(
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
    containingSymbol: FirNamedFunctionSymbol,
  ) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.RENDERER_KEY,
      argName = Name.identifier("value"),
      argument =
        modelClass.classSymbol?.let { buildClassExpression(it, session) }
          ?: buildClassExpression(modelClass.classId, session, owner),
      containingSymbol = containingSymbol,
      session = session,
    )

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRendererFir(session)
  }
}
