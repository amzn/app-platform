package software.amazon.app.platform.metro.compiler.scoped

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
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.resolve.defaultType
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
import org.jetbrains.kotlin.fir.types.impl.ConeClassLikeTypeImpl
import org.jetbrains.kotlin.name.CallableId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import software.amazon.app.platform.metro.compiler.ClassIds
import software.amazon.app.platform.metro.compiler.Keys
import software.amazon.app.platform.metro.compiler.fir.buildAnnotationCallWithArgument
import software.amazon.app.platform.metro.compiler.fir.buildClassExpression
import software.amazon.app.platform.metro.compiler.fir.buildSimpleAnnotationCall
import software.amazon.app.platform.metro.compiler.fir.extractScopeArgument
import software.amazon.app.platform.metro.compiler.fir.extractScopeClassId
import software.amazon.app.platform.metro.compiler.fir.hasAnnotation

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
 *   @Origin(TestClass::class)
 *   interface ScopedContribution {
 *     @Binds
 *     fun bindSuperType(instance: TestClass): SuperType
 *
 *     @Binds
 *     @IntoSet
 *     @ForScope(AppScope::class)
 *     fun bindTestClassScoped(instance: TestClass): Scoped
 *   }
 * }
 * ```
 *
 * No top-level graph interface is generated. If the contributed class only implements `Scoped`,
 * then `bindSuperType()` is omitted and only the scoped multibinding is generated.
 */
public class ContributesScopedFir(session: FirSession) :
  MetroFirDeclarationGenerationExtension(session) {

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesScopedIds.PREDICATE)
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
    if (name != ContributesScopedIds.NESTED_INTERFACE_NAME) return null
    if (!hasAnnotation(owner, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session)) {
      return null
    }

    val scopedOwner = owner as? FirRegularClassSymbol ?: return null
    val metadata = contributesScopedMetadata(scopedOwner, session) ?: return null
    val scopeArg =
      extractScopeArgument(scopedOwner, ContributesScopedIds.CONTRIBUTES_SCOPED_CLASS_ID, session)
        ?: return null
    val nestedClassId = scopedOwner.classId.createNestedClassId(name)
    val contributionSymbol = FirRegularClassSymbol(nestedClassId)

    buildRegularClass {
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
          argument = buildClassExpression(scopedOwner, session),
          containingSymbol = contributionSymbol,
          session = session,
        )
      buildBindingFunctions(nestedClassId, scopedOwner, metadata, scopeArg).forEach { function ->
        declarations += function
      }
    }

    return contributionSymbol
  }

  private fun annotatedScopedClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesScopedIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  private fun buildBindingFunctions(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    metadata: ScopedContributionMetadata,
    scopeArg: org.jetbrains.kotlin.fir.expressions.FirExpression,
  ): List<FirFunction> {
    return buildList {
      metadata.otherSuperType?.let { otherSuperType ->
        add(buildBindFunction(contributionClassId, owner, otherSuperType))
      }
      add(buildBindScopedFunction(contributionClassId, owner, scopeArg))
    }
  }

  private fun buildBindFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    otherSuperType: ResolvedScopedSuperType,
  ): FirFunction {
    val functionName = "bind${ContributesScopedIds.generatedTypeName(otherSuperType.classId)}"
    return buildBindFunction(
      contributionClassId = contributionClassId,
      owner = owner,
      functionName = functionName,
      returnType = otherSuperType.coneType,
    )
  }

  private fun buildBindScopedFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    scopeArg: org.jetbrains.kotlin.fir.expressions.FirExpression,
  ): FirFunction {
    val functionName = "bind${ContributesScopedIds.generatedOwnerName(owner.classId)}Scoped"
    return buildBindFunction(
      contributionClassId = contributionClassId,
      owner = owner,
      functionName = functionName,
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
          )
        )
      },
    )
  }

  private fun buildBindFunction(
    contributionClassId: ClassId,
    owner: FirRegularClassSymbol,
    functionName: String,
    returnType: ConeKotlinType,
    additionalAnnotations:
      MutableList<org.jetbrains.kotlin.fir.expressions.FirAnnotation>.(
        FirNamedFunctionSymbol
      ) -> Unit =
      {},
  ): FirFunction {
    val callableId = CallableId(contributionClassId, Name.identifier(functionName))
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

  private fun contributionType(classId: ClassId) =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(classId),
      emptyArray(),
      isMarkedNullable = false,
    )

  private fun scopedType() =
    ConeClassLikeTypeImpl(
      ConeClassLikeLookupTagImpl(ClassIds.SCOPED),
      emptyArray(),
      isMarkedNullable = false,
    )

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesScopedFir(session)
  }
}
