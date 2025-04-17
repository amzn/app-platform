package software.amazon.app.platform.inject.processor

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
import com.squareup.kotlinpoet.LambdaTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.STAR
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.ksp.addOriginatingKSFile
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.writeTo
import kotlin.reflect.KClass
import me.tatarka.inject.annotations.Inject
import me.tatarka.inject.annotations.IntoMap
import me.tatarka.inject.annotations.Provides
import software.amazon.app.platform.inject.APP_PLATFORM_LOOKUP_PACKAGE
import software.amazon.app.platform.inject.ContextAware
import software.amazon.app.platform.inject.ContributesRenderer
import software.amazon.app.platform.inject.addOriginAnnotation
import software.amazon.lastmile.kotlin.inject.anvil.ContributesTo
import software.amazon.lastmile.kotlin.inject.anvil.ForScope
import software.amazon.lastmile.kotlin.inject.anvil.SingleIn

/**
 * Generates the code for [ContributesRenderer].
 *
 * In the lookup package [APP_PLATFORM_LOOKUP_PACKAGE] a new interface is generated with a provider
 * method for the renderer, e.g.
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
 * package $APP_PLATFORM_LOOKUP_PACKAGE.software.amazon.test
 *
 * @ContributesTo(RendererScope::class)
 * @Origin(TestRenderer::class)
 * interface TestRendererComponent {
 *     @Provides
 *     @IntoMap
 *     fun provideTestRendererIntoMap(
 *         renderer: () -> TestRenderer,
 *     ): Pair<KClass<out BaseModel>, () -> Renderer<*>> = Model::class to renderer
 *
 *     @Provides
 *     fun provideTestRenderer(): TestRenderer = TestRenderer()
 *
 *     @Provides
 *     @IntoMap
 *     @ForScope(RendererScope::class)
 *     fun provideRendererModelKey(): Pair<KClass<out BaseModel>, KClass<out Renderer<*>>> =
 *         Model::class to TestRenderer::class
 * }
 * ```
 */
internal class ContributesRendererProcessor(
  private val codeGenerator: CodeGenerator,
  override val logger: KSPLogger,
) : SymbolProcessor, ContextAware {

  private val baseModel = ClassName("software.amazon.app.platform.presenter", "BaseModel")
  private val baseModelFqName = baseModel.canonicalName

  private val rendererWildcard =
    ClassName("software.amazon.app.platform.renderer", "Renderer").parameterizedBy(STAR)

  private val rendererScope = ClassName("software.amazon.app.platform.renderer", "RendererScope")

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
      .forEach { generateComponentInterface(it) }

    return emptyList()
  }

  @OptIn(KspExperimental::class)
  private fun generateComponentInterface(clazz: KSClassDeclaration) {
    val packageName = "${APP_PLATFORM_LOOKUP_PACKAGE}.${clazz.packageName.asString()}"
    val componentClassName = ClassName(packageName, "${clazz.innerClassNames()}Component")
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
      FileSpec.builder(componentClassName)
        .addType(
          TypeSpec.interfaceBuilder(componentClassName)
            .addOriginatingKSFile(clazz.requireContainingFile())
            .addOriginAnnotation(clazz)
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
      .addParameter(name = "renderer", type = LambdaTypeName.get(returnType = clazz.toClassName()))
      .returns(
        Pair::class.asClassName()
          .parameterizedBy(
            listOf(
              KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(baseModel)),
              LambdaTypeName.get(returnType = rendererWildcard),
            )
          )
      )
      .addStatement("return %T::class·to·renderer", modelType.toClassName())
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
        AnnotationSpec.builder(ForScope::class)
          .addMember("scope = %T::class", rendererScope)
          .build()
      )
      .returns(
        Pair::class.asClassName()
          .parameterizedBy(
            listOf(
              KClass::class.asClassName().parameterizedBy(WildcardTypeName.producerOf(baseModel)),
              KClass::class.asClassName()
                .parameterizedBy(WildcardTypeName.producerOf(rendererWildcard)),
            )
          )
      )
      .addStatement("return %T::class·to·%T::class", modelType.toClassName(), clazz.toClassName())
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
