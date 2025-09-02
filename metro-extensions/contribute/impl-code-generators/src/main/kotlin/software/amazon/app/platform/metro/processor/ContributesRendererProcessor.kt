package software.amazon.app.platform.metro.processor

import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.getAllSuperTypes
import com.google.devtools.ksp.getAnnotationsByType
import com.google.devtools.ksp.isAnnotationPresent
import com.google.devtools.ksp.processing.CodeGenerator
import com.google.devtools.ksp.processing.KSPLogger
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.ForScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.IntoMap
import dev.zacsweers.metro.Provider
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlin.reflect.KClass
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.metro.METRO_LOOKUP_PACKAGE
import software.amazon.app.platform.metro.MetroContextAware
import software.amazon.app.platform.renderer.metro.RendererKey

/**
 * Generates the code for [ContributesRenderer].
 *
 * In the lookup package [METRO_LOOKUP_PACKAGE] a new interface is generated with a provider method
 * for the renderer, e.g.
 *
 * ```
 * package software.amazon.test
 *
 * @ContributesRenderer
 * class TestRenderer : Renderer<Model>
 * ```
 *
 * Will generate:
 * ```
 * package $METRO_LOOKUP_PACKAGE.software.amazon.test
 *
 * @ContributesTo(RendererScope::class)
 * interface TestRendererGraph {
 *     @Provides
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     fun provideTestRendererIntoMap(
 *         renderer: Provider<TestRenderer>,
 *     ): Renderer<*> = renderer()
 *
 *     @Provides
 *     fun provideTestRenderer(): TestRenderer = TestRenderer()
 *
 *     @Provides
 *     @IntoMap
 *     @RendererKey(Model::class)
 *     @ForScope(RendererScope::class)
 *     fun provideRendererModelKey(): KClass<out Renderer<*>> =
 *         TestRenderer::class
 * }
 * ```
 */
internal class ContributesRendererProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, MetroContextAware {

  private val baseModel = ClassName("software.amazon.app.platform.presenter", "BaseModel")
  private val baseModelFqName = baseModel.canonicalName

  private val rendererWildcard =
    ClassName("software.amazon.app.platform.renderer", "Renderer").parameterizedBy(STAR)

  private val rendererScope = ClassName("software.amazon.app.platform.renderer", "RendererScope")
  private val rendererKey = RendererKey::class.asClassName()

  private val singleIn = SingleIn::class.asClassName()

  private val unitFqName = Unit::class.requireQualifiedName()

  override fun process(resolver: Resolver): List<KSAnnotated> {
    resolver
      .getSymbolsWithAnnotation(ContributesRenderer::class)
      .filterIsInstance<KSClassDeclaration>()
      .onEach {
        checkIsPublic(it)
        checkNoSingleton(it)
      }
      .forEach { generateGraphInterface(it) }

    return emptyList()
  }

  @OptIn(KspExperimental::class)
  private fun generateGraphInterface(clazz: KSClassDeclaration) {
    val packageName = "${METRO_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val graphClassName = ClassName(packageName, "${clazz.innerClassNames()}Graph")
    val hasInjectAnnotation = clazz.isAnnotationPresent(Inject::class)

    if (hasInjectAnnotation) {
      checkNoZeroArgConstructor(clazz)
    } else {
      checkZeroArgConstructor(clazz)
    }

    val includeSealedSubtypes =
      try {
        clazz.getAnnotationsByType(ContributesRenderer::class).single().includeSealedSubtypes
      } catch (_: NoSuchElementException) {
        /*
        Caused by: java.util.NoSuchElementException: Collection contains no element matching the predicate.
          at com.google.devtools.ksp.UtilsKt.createInvocationHandler$lambda$8(utils.kt:591)
          at jdk.proxy105/jdk.proxy105.$Proxy1029.includeSealedSubtypes(Unknown Source)
          at software.amazon.app.platform.inject.processor.ContributesRendererProcessor.generateComponentInterface(ContributesRendererProcessor.kt:120)

        We're seeing this exception when trying to read 'includeSealedSubtypes' for an annotation
        where the value is not declared, e.g. '@ContributesRenderer' (without any arguments).
        This happens only on iOS for some reason. Fallback to the default value 'true'.
         */
        true
      }

    val allModels =
      if (includeSealedSubtypes) {
        generateSequence(listOf(modelType(clazz))) { classes ->
            classes.flatMap { it.getSealedSubclasses() }.takeIf { it.isNotEmpty() }
          }
          .flatten()
      } else {
        sequenceOf(modelType(clazz))
      }

    val fileSpec =
      FileSpec.builder(graphClassName)
        .addType(
          TypeSpec.interfaceBuilder(graphClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addAnnotation(
              AnnotationSpec.builder(ContributesTo::class)
                .addMember("%T::class", rendererScope)
                .build()
            )
            .apply {
              if (!hasInjectAnnotation) {
                addFunction(
                  FunSpec.builder("provide${clazz.safeClassName}")
                    .addAnnotation(Provides::class)
                    .returns(clazz.toClassName())
                    .addStatement("return %T()", clazz.toClassName())
                    .build()
                )
              }
            }
            .addFunctions(allModels.map { createModelBindingFunction(clazz, it) }.toList())
            .addFunctions(allModels.map { createModelKeyFunction(clazz, it) }.toList())
            .build()
        )
        .build()

    fileSpec.writeTo(codeGenerator, aggregating = false)
  }

  private fun modelType(clazz: KSClassDeclaration): KSClassDeclaration {
    val annotation = clazz.findAnnotation(ContributesRenderer::class)
    val explicitModelType =
      (annotation.arguments.firstOrNull { it.name?.asString() == "modelType" }
          ?: annotation.arguments.firstOrNull())
        ?.let { (it.value as? KSType)?.declaration as? KSClassDeclaration }
        ?.takeIf { it.requireQualifiedName() != unitFqName }

    if (explicitModelType != null) {
      return explicitModelType
    }

    val implicitModelTypes =
      clazz
        .getAllSuperTypes()
        .flatMap { superType ->
          superType.arguments.filter { it.type?.resolve()?.extendsBaseModel() ?: false }
        }
        .mapNotNull { it.type?.resolve()?.declaration as? KSClassDeclaration }
        .distinctBy { it.requireQualifiedName() }
        .toList()

    check(implicitModelTypes.size == 1, clazz) {
      buildString {
        append(
          "Couldn't find BaseModel type for ${clazz.simpleName.asString()}. " +
            "Consider adding an explicit parameter."
        )
        if (implicitModelTypes.size > 1) {
          append("Found: ")
          append(implicitModelTypes.joinToString { it.requireQualifiedName() })
        }
      }
    }

    return implicitModelTypes[0]
  }

  private fun createModelBindingFunction(
    clazz: KSClassDeclaration,
    modelType: KSClassDeclaration,
  ): FunSpec {
    return FunSpec.builder("provide${clazz.safeClassName}" + modelType.innerClassNames())
      .addAnnotation(Provides::class)
      .addAnnotation(IntoMap::class)
      .addAnnotation(
        AnnotationSpec.builder(rendererKey).addMember("%T::class", modelType.toClassName()).build()
      )
      .addParameter(
        name = "renderer",
        type = Provider::class.asClassName().parameterizedBy(clazz.toClassName()),
      )
      .returns(rendererWildcard)
      .addStatement("return renderer()")
      .build()
  }

  private fun createModelKeyFunction(
    clazz: KSClassDeclaration,
    modelType: KSClassDeclaration,
  ): FunSpec {
    return FunSpec.builder("provide${clazz.safeClassName}" + modelType.innerClassNames() + "Key")
      .addAnnotation(Provides::class)
      .addAnnotation(IntoMap::class)
      .addAnnotation(
        AnnotationSpec.builder(rendererKey).addMember("%T::class", modelType.toClassName()).build()
      )
      .addAnnotation(
        AnnotationSpec.builder(ForScope::class)
          .addMember("scope = %T::class", rendererScope)
          .build()
      )
      .returns(
        KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(rendererWildcard))
      )
      .addStatement("return %T::class", clazz.toClassName())
      .build()
  }

  private fun checkNoSingleton(clazz: KSClassDeclaration) {
    val hasSingleInAnnotation =
      clazz.annotations.any { annotation ->
        annotation.isAnnotation(singleIn.canonicalName) &&
          clazz.scope().type.declaration.requireQualifiedName() == rendererScope.canonicalName
      }

    if (hasSingleInAnnotation) {
      logger.error(
        "Renderers should not be singletons in the RendererScope. The " +
          "RendererFactory will cache the Renderer when necessary. Remove the " +
          "@SingleIn(RendererScope::class) annotation.",
        clazz,
      )
    }
  }

  private fun checkNoZeroArgConstructor(clazz: KSClassDeclaration) {
    val parameterCount = clazz.primaryConstructor?.parameters?.size ?: 0
    check(parameterCount > 0, clazz) {
      "It's redundant to use @Inject when using " +
        "@ContributesRenderer for a Renderer with a zero-arg constructor."
    }
  }

  private fun checkZeroArgConstructor(clazz: KSClassDeclaration) {
    val parameterCount = clazz.primaryConstructor?.parameters?.size ?: 0
    check(parameterCount == 0, clazz) {
      "When using @ContributesRenderer and you need to inject types in the constructor, " +
        "then it's necessary to add the @Inject annotation."
    }
  }

  private fun KSType.extendsBaseModel(): Boolean {
    val superTypes =
      (this.declaration as? KSClassDeclaration)?.getAllSuperTypes() ?: emptySequence()

    return superTypes.any { it.declaration.qualifiedName?.asString() == baseModelFqName }
  }
}
