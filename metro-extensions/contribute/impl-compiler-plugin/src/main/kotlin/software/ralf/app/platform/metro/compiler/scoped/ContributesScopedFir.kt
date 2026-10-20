package software.ralf.app.platform.metro.compiler.scoped

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
import org.jetbrains.kotlin.fir.types.coneType
import org.jetbrains.kotlin.fir.types.constructClassLikeType
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys
import software.ralf.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.ralf.app.platform.metro.compiler.fir.buildClassExpression
import software.ralf.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.ralf.app.platform.metro.compiler.fir.extractScopeArgument
import software.ralf.app.platform.metro.compiler.fir.extractScopeClassId
import software.ralf.app.platform.metro.compiler.fir.hasAnnotation
import software.ralf.app.platform.metro.compiler.fir.resolveTypeRef

/**
 * Generates the declaration shape for `@ContributesScoped` classes.
 *
 * Pseudo Kotlin:
 * ```kotlin
 * @Inject
 * @ContributesScoped(AppScope::class)
 * class TestClass : SuperType, Scoped {
 *
 *   @ContributesTo(AppScope::class)
 *   @BindingContainer
 *   @Origin(TestClass::class)
 *   interface ScopedContribution {
 *     @Binds
 *     fun bindSuperType(instance: TestClass): SuperType
 *
 *     @Binds
 *     @IntoSet
 *     @ForScope(AppScope::class)
 *     fun bindTestClassScoped(instance: TestClass): Scoped
 *
 *     companion object {
 *       @Provides
 *       fun provideTestClass(dependency: Dependency): TestClass
 *     }
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the scoped class is already `@Inject`
 * constructible, the nested declaration omits `provideTestClass(...)` and only synthesizes the
 * binding methods. If the contributed class only implements `Scoped`, then `bindSuperType()` is
 * omitted and only the scoped multibinding is generated.
 */
public class ContributesScopedFir
internal constructor(
  session: FirSession,
  private val compatContext: CompatContext,
) : MetroFirDeclarationGenerationExtension(session), MetroContributionHintExtension {
  public constructor(session: FirSession) : this(session, CompatContext.create())

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesScopedIds.PREDICATE)
    register(ContributesScopedIds.SINGLE_IN_PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedScopedClasses().mapNotNull { classSymbol ->
      if (contributesScopedMetadata(classSymbol, session) == null) return@mapNotNull null

      val scopeClassId =
        extractScopeClassId(classSymbol, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session)
          ?: return@mapNotNull null
      ContributionHint(
        contributingClassId =
          classSymbol.classId.createNestedClassId(ContributesScopedIds.NESTED_INTERFACE_NAME),
        scope = scopeClassId,
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    generatedScopedContributionOwner(classSymbol)?.let { scopedOwner ->
      return if (hasScopedInjectAnnotation(scopedOwner, session)) {
        emptySet()
      } else {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      }
    }

    val regularClass = classSymbol as? FirRegularClassSymbol ?: return emptySet()
    return if (
      hasAnnotation(classSymbol, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session) &&
        contributesScopedMetadata(regularClass, session) != null
    ) {
      setOf(ContributesScopedIds.NESTED_INTERFACE_NAME)
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
      val scopedOwner = generatedScopedContributionOwner(owner) ?: return null
      if (hasScopedInjectAnnotation(scopedOwner, session)) return null
      val contributionSymbol = owner as? FirRegularClassSymbol ?: return null
      return createCompanionObject(contributionSymbol, Keys.ContributesScopedGeneratorKey).symbol
    }

    if (name != ContributesScopedIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session)) {
      return null
    }

    val scopedOwner = owner as? FirRegularClassSymbol ?: return null
    if (contributesScopedMetadata(scopedOwner, session) == null) return null
    val scopeArg =
      extractScopeArgument(scopedOwner, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session)
        ?: return null
    val nestedClassId = scopedOwner.classId.createNestedClassId(name)
    val contributionSymbol = FirRegularClassSymbol(nestedClassId)

    val contributionClass = buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesScopedGeneratorKey.origin
      source = scopedOwner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = contributionSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(scopedOwner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations +=
        buildSimpleAnnotationCall(ClassIds.BINDING_CONTAINER, contributionSymbol, session)
      annotations +=
        buildAnnotationCallWithArgument(
          classId = ClassIds.CONTRIBUTES_TO,
          argName = Name.identifier("scope"),
          argument = scopeArg,
          containingSymbol = contributionSymbol,
          session = session,
        )
      annotations +=
        buildAnnotationCallWithArgument(
          classId = ClassIds.ORIGIN,
          argName = Name.identifier("value"),
          argument = buildClassExpression(scopedOwner, session, compatContext),
          containingSymbol = contributionSymbol,
          session = session,
        )
    }

    return contributionClass.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    generatedScopedContributionOwner(classSymbol)?.let { scopedOwner ->
      val metadata = contributesScopedMetadata(scopedOwner, session) ?: return emptySet()
      return buildSet {
        metadata.otherSuperType?.let { add(bindFunctionName(it)) }
        add(bindScopedFunctionName(scopedOwner))
      }
    }
    generatedScopedProviderCompanionOwner(classSymbol)?.let { scopedOwner ->
      return setOf(SpecialNames.INIT, provideFunctionName(scopedOwner))
    }
    return emptySet()
  }

  override fun generateFunctions(
    callableId: CallableId,
    context: MemberGenerationContext?,
  ): List<FirNamedFunctionSymbol> {
    val classSymbol = context?.owner ?: return emptyList()
    generatedScopedContributionOwner(classSymbol)?.let { scopedOwner ->
      val metadata = contributesScopedMetadata(scopedOwner, session) ?: return emptyList()
      return buildList {
        metadata.otherSuperType?.let { otherSuperType ->
          if (callableId.callableName == bindFunctionName(otherSuperType)) {
            add(
              buildBindFunction(
                  contributionClassId = classSymbol.classId,
                  owner = scopedOwner,
                  otherSuperType = otherSuperType,
                )
                .symbol,
            )
          }
        }
        if (callableId.callableName == bindScopedFunctionName(scopedOwner)) {
          val scopeArg =
            extractScopeArgument(
              classSymbol = scopedOwner,
              annotationClassId = ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID,
              session = session,
            ) ?: return emptyList()
          add(
            buildBindScopedFunction(
                contributionClassId = classSymbol.classId,
                owner = scopedOwner,
                scopeArg = scopeArg,
              )
              .symbol,
          )
        }
      }
    }
    generatedScopedProviderCompanionOwner(classSymbol)?.let { scopedOwner ->
      if (callableId.callableName == provideFunctionName(scopedOwner)) {
        return listOf(
          buildProvideScopedFunction(contributionClassId = classSymbol.classId, owner = scopedOwner)
            .symbol,
        )
      }
    }
    return emptyList()
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (generatedScopedProviderCompanionOwner(context.owner) == null) {
      return emptyList()
    }
    val constructor: FirConstructor =
      createDefaultPrivateConstructor(context.owner, Keys.ContributesScopedGeneratorKey)
    return listOf(constructor.symbol)
  }

  private fun generatedScopedContributionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != ContributesScopedIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = classSymbol.classId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session) &&
        contributesScopedMetadata(it, session) != null
    }
  }

  private fun generatedScopedProviderCompanionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      return null
    }
    val contributionClassId = classSymbol.classId.outerClassId ?: return null
    if (contributionClassId.shortClassName != ContributesScopedIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = contributionClassId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session) &&
        contributesScopedMetadata(it, session) != null &&
        !hasScopedInjectAnnotation(it, session)
    }
  }

  private fun annotatedScopedClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesScopedIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildProvideScopedFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
  ): FirNamedFunction {
    val callableId = CallableId(contributionClassId, provideFunctionName(owner))
    val functionSymbol = FirNamedFunctionSymbol(callableId)
    val constructor = scopedConstructor(owner)
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
      origin = Keys.ContributesScopedGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
      dispatchReceiverType = contributionType(contributionClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      annotations += buildSimpleAnnotationCall(ClassIds.PROVIDES, functionSymbol, session)
      extractScopeClassId(owner, ClassIds.SINGLE_IN, session)?.let { scopeClassId ->
        annotations +=
          buildAnnotationCallWithArgument(
            ClassIds.SINGLE_IN,
            Name.identifier("scope"),
            buildClassExpression(scopeClassId, session, compatContext, owner),
            functionSymbol,
            session,
          )
      }
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

  private fun buildBindFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    otherSuperType: ResolvedScopedSuperType,
  ): FirNamedFunction {
    return buildBindFunction(
      contributionClassId = contributionClassId,
      owner = owner,
      functionName = bindFunctionName(otherSuperType),
      returnType = otherSuperType.coneType,
    )
  }

  private fun buildBindScopedFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    scopeArg: org.jetbrains.kotlin.fir.expressions.FirExpression,
  ): FirNamedFunction {
    return buildBindFunction(
      contributionClassId = contributionClassId,
      owner = owner,
      functionName = bindScopedFunctionName(owner),
      returnType = scopedType(),
      additionalAnnotations = { functionSymbol ->
        add(buildSimpleAnnotationCall(ClassIds.INTO_SET, functionSymbol, session))
        add(
          buildAnnotationCallWithArgument(
            classId = ClassIds.FOR_SCOPE,
            argName = Name.identifier("scope"),
            argument = scopeArg,
            containingSymbol = functionSymbol,
            session = session,
          ),
        )
      },
    )
  }

  private fun buildBindFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    functionName: Name,
    returnType: ConeKotlinType,
    additionalAnnotations:
      MutableList<org.jetbrains.kotlin.fir.expressions.FirAnnotation>.(
        FirNamedFunctionSymbol,
      ) -> Unit =
      {},
  ): FirNamedFunction {
    val callableId = CallableId(contributionClassId, functionName)
    val functionSymbol = FirNamedFunctionSymbol(callableId)

    return buildNamedFunction {
      isLocal = false
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesScopedGeneratorKey.origin
      source = owner.source
      symbol = functionSymbol
      name = callableId.callableName
      returnTypeRef = returnType.toFirResolvedTypeRef()
      dispatchReceiverType = contributionType(contributionClassId)
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.OPEN,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      valueParameters += buildValueParameter {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesScopedGeneratorKey.origin
        source = owner.source
        returnTypeRef = owner.defaultType().toFirResolvedTypeRef()
        name = Name.identifier("instance")
        symbol = FirValueParameterSymbol()
        containingDeclarationSymbol = functionSymbol
      }
      annotations += buildSimpleAnnotationCall(ClassIds.BINDS, functionSymbol, session)
      annotations.additionalAnnotations(functionSymbol)
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
      origin = Keys.ContributesScopedGeneratorKey.origin
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

  private fun bindFunctionName(otherSuperType: ResolvedScopedSuperType): Name =
    Name.identifier("bind${ContributesScopedIds.generatedTypeName(otherSuperType.classId)}")

  private fun bindScopedFunctionName(owner: FirRegularClassSymbol): Name =
    Name.identifier("bind${ContributesScopedIds.generatedOwnerName(owner.classId)}Scoped")

  private fun provideFunctionName(owner: FirRegularClassSymbol): Name =
    Name.identifier("provide${ContributesScopedIds.generatedOwnerName(owner.classId)}")

  private fun contributionType(classId: ClassId) = classId.constructClassLikeType()

  private fun scopedType() = ClassIds.SCOPED.constructClassLikeType()

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesScopedFir(session, compatContext)
  }
}
