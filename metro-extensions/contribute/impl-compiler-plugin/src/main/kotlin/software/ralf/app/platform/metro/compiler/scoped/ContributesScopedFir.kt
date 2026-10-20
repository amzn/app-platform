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
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.extensions.FirDeclarationPredicateRegistrar
import org.jetbrains.kotlin.fir.extensions.MemberGenerationContext
import org.jetbrains.kotlin.fir.extensions.NestedClassGenerationContext
import org.jetbrains.kotlin.fir.extensions.predicateBasedProvider
import org.jetbrains.kotlin.fir.moduleData
import org.jetbrains.kotlin.fir.plugin.createCompanionObject
import org.jetbrains.kotlin.fir.plugin.createDefaultPrivateConstructor
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.scopes.kotlinScopeProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
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

/** Generates contribution interfaces and provider companions for `@ContributesScoped` classes. */
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
    return if (generatedScopedProviderCompanionOwner(classSymbol) != null) {
      setOf(SpecialNames.INIT)
    } else {
      emptySet()
    }
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

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesScopedFir(session, compatContext)
  }
}
