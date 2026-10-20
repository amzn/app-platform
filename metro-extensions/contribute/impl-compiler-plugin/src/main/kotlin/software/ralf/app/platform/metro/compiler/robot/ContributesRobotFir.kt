package software.ralf.app.platform.metro.compiler.robot

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
import org.jetbrains.kotlin.fir.declarations.DirectDeclarationsAccess
import org.jetbrains.kotlin.fir.declarations.FirConstructor
import org.jetbrains.kotlin.fir.declarations.FirResolvePhase
import org.jetbrains.kotlin.fir.declarations.builder.buildRegularClass
import org.jetbrains.kotlin.fir.declarations.impl.FirResolvedDeclarationStatusImpl
import org.jetbrains.kotlin.fir.declarations.origin
import org.jetbrains.kotlin.fir.declarations.toAnnotationClassIdSafe
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
import org.jetbrains.kotlin.fir.symbols.SymbolInternals
import org.jetbrains.kotlin.fir.symbols.impl.FirClassLikeSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirConstructorSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirRegularClassSymbol
import org.jetbrains.kotlin.fir.toEffectiveVisibility
import org.jetbrains.kotlin.fir.toFirResolvedTypeRef
import org.jetbrains.kotlin.fir.types.constructClassLikeType
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

/** Generates the nested contribution interfaces and provider companion for `@ContributesRobot`. */
public class ContributesRobotFir
internal constructor(
  session: FirSession,
  private val compatContext: CompatContext,
) : MetroFirDeclarationGenerationExtension(session), MetroContributionHintExtension {
  public constructor(session: FirSession) : this(session, CompatContext.create())

  override fun FirDeclarationPredicateRegistrar.registerPredicates() {
    register(ContributesRobotIds.PREDICATE)
  }

  override fun getContributionHints(): List<ContributionHint> {
    return annotatedRobotClasses().flatMap { classSymbol ->
      val scopeClassId =
        extractScopeClassId(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
          ?: return@flatMap emptyList()
      listOf(
        ContributionHint(
          contributingClassId =
            classSymbol.classId.createNestedClassId(ContributesRobotIds.NESTED_INTERFACE_NAME),
          scope = scopeClassId,
        ),
        ContributionHint(
          contributingClassId =
            classSymbol.classId.createNestedClassId(
              ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME,
            ),
          scope = scopeClassId,
        ),
      )
    }
  }

  override fun getNestedClassifiersNames(
    classSymbol: FirClassSymbol<*>,
    context: NestedClassGenerationContext,
  ): Set<Name> {
    generatedRobotContributionOwner(classSymbol)?.let { owner ->
      return if (hasInjectAnnotation(owner)) {
        emptySet()
      } else {
        setOf(SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT)
      }
    }

    return if (
      hasAnnotation(classSymbol, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    ) {
      setOf(
        ContributesRobotIds.NESTED_INTERFACE_NAME,
        ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME,
      )
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
      val robotOwner = generatedRobotContributionOwner(owner) ?: return null
      if (hasInjectAnnotation(robotOwner)) return null
      val contributionSymbol = owner as? FirRegularClassSymbol ?: return null
      return createCompanionObject(contributionSymbol, Keys.ContributesRobotGeneratorKey).symbol
    }

    if (!hasAnnotation(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)) return null

    val scopeArg =
      extractScopeArgument(owner, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
        ?: return null

    if (name == ContributesRobotIds.NESTED_ROBOT_GRAPH_INTERFACE_NAME) {
      val nestedClassId = owner.classId.createNestedClassId(name)
      val classSymbol = FirRegularClassSymbol(nestedClassId)
      val contributionClass = buildRegularClass {
        resolvePhase = FirResolvePhase.BODY_RESOLVE
        moduleData = session.moduleData
        origin = Keys.ContributesRobotGeneratorKey.origin
        source = owner.source
        classKind = ClassKind.INTERFACE
        scopeProvider = session.kotlinScopeProvider
        this.name = nestedClassId.shortClassName
        symbol = classSymbol
        status =
          FirResolvedDeclarationStatusImpl(
            Visibilities.Public,
            Modality.ABSTRACT,
            Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
          )
        superTypeRefs += robotGraphType().toFirResolvedTypeRef()
        annotations +=
          buildAnnotationCallWithArgument(
            ClassIds.CONTRIBUTES_TO,
            Name.identifier("scope"),
            scopeArg,
            classSymbol,
            session,
          )
        annotations += buildOriginAnnotation(owner)
      }
      return contributionClass.symbol
    }

    if (name != ContributesRobotIds.NESTED_INTERFACE_NAME) return null

    val nestedClassId = owner.classId.createNestedClassId(name)
    val classSymbol = FirRegularClassSymbol(nestedClassId)

    val contributionClass = buildRegularClass {
      resolvePhase = FirResolvePhase.BODY_RESOLVE
      moduleData = session.moduleData
      origin = Keys.ContributesRobotGeneratorKey.origin
      source = owner.source
      classKind = ClassKind.INTERFACE
      scopeProvider = session.kotlinScopeProvider
      this.name = nestedClassId.shortClassName
      symbol = classSymbol
      status =
        FirResolvedDeclarationStatusImpl(
          Visibilities.Public,
          Modality.ABSTRACT,
          Visibilities.Public.toEffectiveVisibility(owner, forClass = true),
        )
      superTypeRefs += session.builtinTypes.anyType
      annotations += buildSimpleAnnotationCall(ClassIds.BINDING_CONTAINER, classSymbol, session)
      annotations +=
        buildAnnotationCallWithArgument(
          ClassIds.CONTRIBUTES_TO,
          Name.identifier("scope"),
          scopeArg,
          classSymbol,
          session,
        )
      annotations += buildOriginAnnotation(owner)
    }

    return contributionClass.symbol
  }

  override fun getCallableNamesForClass(
    classSymbol: FirClassSymbol<*>,
    context: MemberGenerationContext,
  ): Set<Name> {
    return if (generatedRobotProviderCompanionOwner(classSymbol) != null) {
      setOf(SpecialNames.INIT)
    } else {
      emptySet()
    }
  }

  override fun generateConstructors(context: MemberGenerationContext): List<FirConstructorSymbol> {
    if (generatedRobotProviderCompanionOwner(context.owner) == null) {
      return emptyList()
    }
    val constructor: FirConstructor =
      createDefaultPrivateConstructor(context.owner, Keys.ContributesRobotGeneratorKey)
    return listOf(constructor.symbol)
  }

  private fun generatedRobotContributionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != ContributesRobotIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = classSymbol.classId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session)
    }
  }

  private fun generatedRobotProviderCompanionOwner(
    classSymbol: FirClassSymbol<*>,
  ): FirRegularClassSymbol? {
    if (classSymbol.classId.shortClassName != SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT) {
      return null
    }
    val contributionClassId = classSymbol.classId.outerClassId ?: return null
    if (contributionClassId.shortClassName != ContributesRobotIds.NESTED_INTERFACE_NAME) {
      return null
    }
    val ownerClassId = contributionClassId.outerClassId ?: return null
    val ownerSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ownerClassId) as? FirRegularClassSymbol
        ?: return null
    return ownerSymbol.takeIf {
      hasAnnotation(it, ContributesRobotIds.CONTRIBUTES_ROBOT_CLASS_ID, session) &&
        !hasInjectAnnotation(it)
    }
  }

  private fun annotatedRobotClasses(): List<FirRegularClassSymbol> {
    return session.predicateBasedProvider
      .getSymbolsByPredicate(ContributesRobotIds.PREDICATE)
      .filterIsInstance<FirRegularClassSymbol>()
      .toList()
  }

  @OptIn(DirectDeclarationsAccess::class, SymbolInternals::class)
  private fun hasInjectAnnotation(owner: FirClassSymbol<*>): Boolean {
    if (hasAnnotation(owner, ClassIds.INJECT, session)) return true
    val ownerClass = owner as? FirRegularClassSymbol ?: return false
    return ownerClass.declarationSymbols.filterIsInstance<FirConstructorSymbol>().any { constructor
      ->
      constructor.fir.annotations.any { it.toAnnotationClassIdSafe(session) == ClassIds.INJECT }
    }
  }

  private fun robotGraphType() = ClassIds.ROBOT_GRAPH.constructClassLikeType()

  private fun buildOriginAnnotation(owner: FirClassSymbol<*>) = buildAnnotation {
    val originSymbol =
      session.symbolProvider.getClassLikeSymbolByClassId(ClassIds.ORIGIN) as? FirRegularClassSymbol
        ?: error("Annotation class ${ClassIds.ORIGIN} not found on the classpath")
    annotationTypeRef = originSymbol.defaultType().toFirResolvedTypeRef()
    argumentMapping = buildAnnotationArgumentMapping {
      mapping[Name.identifier("value")] = buildClassExpression(owner, session, compatContext)
    }
  }

  @AutoService(MetroFirDeclarationGenerationExtension.Factory::class)
  public class Factory : MetroFirDeclarationGenerationExtension.Factory {
    override fun create(
      session: FirSession,
      options: MetroOptions,
      compatContext: CompatContext,
    ): MetroFirDeclarationGenerationExtension = ContributesRobotFir(session, compatContext)
  }
}
