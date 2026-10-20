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
import org.jetbrains.kotlin.fir.FirFunctionTarget
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirNamedFunction
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.FirValueParameter
import org.jetbrains.kotlin.fir.declarations.builder.buildNamedFunction
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.builder.buildValueParameter
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.expressions.FirExpression
import org.jetbrains.kotlin.fir.expressions.buildResolvedArgumentList
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotation
import org.jetbrains.kotlin.fir.expressions.builder.buildAnnotationArgumentMapping
import org.jetbrains.kotlin.fir.expressions.builder.buildFunctionCall
import org.jetbrains.kotlin.fir.expressions.builder.buildPropertyAccessExpression
import org.jetbrains.kotlin.fir.expressions.builder.buildReturnExpression
import org.jetbrains.kotlin.fir.expressions.impl.buildSingleExpressionBlock
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.references.builder.buildResolvedNamedReference
import org.jetbrains.kotlin.fir.resolve.defaultType
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirNamedFunctionSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirValueParameterSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.ConeKotlinType
import org.jetbrains.kotlin.fir.types.ConeKotlinTypeProjectionOut
import org.jetbrains.kotlin.fir.types.ConeStarProjection
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys
import software.ralf.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.ralf.app.platform.metro.compiler.fir.buildClassExpression
import software.ralf.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveTypeRef

/**
 * Generates the declaration shape for `@ContributesRenderer` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model> {
 *
 *   @ContributesTo(RendererScope::class)
 *   @BindingContainer
 *   @Origin(TestRenderer::class)
 *   interface RendererContribution {
 *     @Binds
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererModel(renderer: TestRenderer): Renderer<*>
 *
 *     companion object {
 *       @Provides
 *       fun provideTestRenderer(): TestRenderer
 *
 *       @Provides
 *       @IntoMap
 *       @RendererKey(Model::class)
 *       @ForScope(RendererScope::class)
 *       fun provideTestRendererModelKey(): KClass<out Renderer<*>>
 *     }
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the renderer has an `@Inject` constructor, the
 * nested declaration omits `provideTestRenderer()` and only the map bindings are generated. When
 * `includeSealedSubtypes` is enabled, the `Model` binding pair is generated once per collected
 * model subtype.
 */
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
          classSymbol.classId.createNestedClassId(ContributesRendererIds.NESTED_INTERFACE_NAME),
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
    generatedRendererContributionOwner(classSymbol)?.let { rendererOwner ->
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptySet()
      return metadata.modelClasses.mapTo(linkedSetOf()) { rendererBindingName(rendererOwner, it) }
    }

    val rendererOwner = generatedRendererProviderCompanionOwner(classSymbol) ?: return emptySet()
    val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptySet()
    return buildSet {
      add(SpecialNames.INIT)
      if (!metadata.hasInjectAnnotation) {
        add(rendererProviderName(rendererOwner))
      }
      metadata.modelClasses.forEach { add(rendererKeyName(rendererOwner, it)) }
    }
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?,
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()
    generatedRendererContributionOwner(classSymbol)?.let { rendererOwner ->
      val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptyList()
      return metadata.modelClasses
        .filter { rendererBindingName(rendererOwner, it) == callableId.callableName }
        .map {
          buildProvideRendererIntoMapFunction(
              graphClassId = classSymbol.classId,
              owner = rendererOwner,
              modelClass = it,
            )
            .symbol
        }
    }

    val rendererOwner = generatedRendererProviderCompanionOwner(classSymbol) ?: return emptyList()
    val metadata = rendererContributionMetadata(rendererOwner, session) ?: return emptyList()
    return buildList {
      if (
        !metadata.hasInjectAnnotation &&
          callableId.callableName == rendererProviderName(rendererOwner)
      ) {
        add(buildProvideRendererFunction(classSymbol.classId, rendererOwner).symbol)
      }
      metadata.modelClasses
        .filter { rendererKeyName(rendererOwner, it) == callableId.callableName }
        .forEach {
          add(
            buildProvideRendererKeyFunction(
                graphClassId = classSymbol.classId,
                owner = rendererOwner,
                modelClass = it,
              )
              .symbol,
          )
        }
    }
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
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

  private fun rendererProviderName(owner: FirRegularClassSymbol): Name =
    Name.identifier("provide${ContributesRendererIds.generatedSafeClassNamePrefix(owner.classId)}")

  private fun rendererBindingName(
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): Name =
    Name.identifier(
      rendererProviderName(owner).asString() +
        ContributesRendererIds.generatedModelClassNameSuffix(modelClass.classId),
    )

  private fun rendererKeyName(owner: FirRegularClassSymbol, modelClass: ResolvedModelClass): Name =
    Name.identifier(rendererBindingName(owner, modelClass).asString() + "Key")

  private fun buildProvideRendererFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
  ): FirNamedFunction {
    val callableId = CallableId(graphClassId, rendererProviderName(owner))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val constructor = rendererConstructor(owner)
    val generatedParameters =
      constructor
        ?.parameters
        ?.map { it.toGeneratedParameter(constructor.owner, functionSymbol) }
        .orEmpty()
    var returnTarget: FirFunctionTarget? = null

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
      valueParameters += generatedParameters
      if (constructor != null) {
        body =
          buildSingleExpressionBlock(
            buildReturnExpression {
              val target = FirFunctionTarget(labelName = null, isLambda = false)
              returnTarget = target
              this.target = target
              result =
                buildConstructorCall(
                  owner,
                  constructor.symbol,
                  constructor.parameters,
                  generatedParameters,
                )
            },
          )
      }
    }
      .also { function -> returnTarget?.bind(function) }
  }

  private fun buildProvideRendererIntoMapFunction(
    graphClassId: ClassId,
    owner: FirRegularClassSymbol,
    modelClass: ResolvedModelClass,
  ): FirNamedFunction {
    val callableId = CallableId(graphClassId, rendererBindingName(owner, modelClass))
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
  ): FirNamedFunction {
    val callableId = CallableId(graphClassId, rendererKeyName(owner, modelClass))
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

  private fun FirValueParameter.toGeneratedParameter(
    owner: FirRegularClassSymbol,
    functionSymbol: FirNamedFunctionSymbol,
  ): FirValueParameter {
    val constructorParameter = this
    return buildValueParameter {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRendererGeneratorKey.origin
      source = null
      returnTypeRef =
        resolveTypeRef(constructorParameter.returnTypeRef, owner, session)?.toFirResolvedTypeRef()
          ?: constructorParameter.returnTypeRef
      name = constructorParameter.name
      symbol = FirValueParameterSymbol()
      containingDeclarationSymbol = functionSymbol
      isCrossinline = constructorParameter.isCrossinline
      isNoinline = constructorParameter.isNoinline
      isVararg = constructorParameter.isVararg
      annotations += constructorParameter.annotations
    }
  }

  private fun buildConstructorCall(
    owner: FirClassSymbol<*>,
    constructorSymbol: FirConstructorSymbol,
    constructorParameters: List<FirValueParameter>,
    generatedParameters: List<FirValueParameter>,
  ): FirExpression {
    return buildFunctionCall {
      coneTypeOrNull = owner.defaultType()
      calleeReference = buildResolvedNamedReference {
        name = owner.name
        resolvedSymbol = constructorSymbol
      }
      argumentList =
        buildResolvedArgumentList(
          original = null,
          mapping =
            generatedParameters.zip(constructorParameters).associateTo(LinkedHashMap()) {
              (generatedParameter, constructorParameter) ->
              buildParameterAccess(generatedParameter) to constructorParameter
            },
        )
    }
  }

  private fun buildParameterAccess(parameter: FirValueParameter): FirExpression {
    return buildPropertyAccessExpression {
      source = parameter.source
      coneTypeOrNull = parameter.returnTypeRef.coneType
      calleeReference = buildResolvedNamedReference {
        name = parameter.name
        resolvedSymbol = parameter.symbol
      }
    }
  }

  private fun generatedGraphType(graphClassId: ClassId) = graphClassId.constructClassLikeType()

  private fun rendererStarType() =
    ClassIds.RENDERER.constructClassLikeType(arrayOf(ConeStarProjection))

  private fun kClassProducerOf(type: ConeKotlinType) =
    ClassId(FqName("kotlin.reflect"), Name.identifier("KClass"))
      .constructClassLikeType(arrayOf(ConeKotlinTypeProjectionOut(type)))

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

  private fun buildForScopeAnnotation(containingSymbol: FirNamedFunctionSymbol) =
    buildAnnotationCallWithArgument(
      classId = ClassIds.FOR_SCOPE,
      argName = Name.identifier("scope"),
      argument = buildClassExpression(ClassIds.RENDERER_SCOPE, session, compatContext),
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
      argument = buildClassExpression(owner, session, compatContext),
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
        modelClass.classSymbol?.let { buildClassExpression(it, session, compatContext) }
          ?: buildClassExpression(modelClass.classId, session, compatContext, owner),
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
