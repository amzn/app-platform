package software.ralf.app.platform.metro.compiler.renderer

import dev.zacsweers.metro.compiler.compat.CompatContext
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrParameterKind
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.UnsafeDuringIrConstructionAPI
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.IrTypeProjection
import org.jetbrains.kotlin.ir.types.IrTypeSubstitutor
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.impl.makeTypeProjection
import org.jetbrains.kotlin.ir.types.starProjectedType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.types.typeWithArguments
import org.jetbrains.kotlin.ir.util.classId
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.copyTo
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.types.Variance
import software.ralf.app.platform.metro.compiler.ClassIds
import software.ralf.app.platform.metro.compiler.Keys

@Suppress("DEPRECATION")
internal class ContributesRendererIrExtension(private val compatContext: CompatContext) :
  IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    moduleFragment.transformChildrenVoid(
      ContributesRendererIrTransformer(pluginContext, compatContext),
    )
  }

  @OptIn(UnsafeDuringIrConstructionAPI::class)
  private class ContributesRendererIrTransformer(
    private val pluginContext: IrPluginContext,
    private val compatContext: CompatContext,
  ) : IrElementTransformerVoid(), CompatContext by compatContext {

    private val metadataRegistrar = createIrGeneratedDeclarationsRegistrar(pluginContext)
    private val rendererType by lazy {
      requireNotNull(pluginContext.referenceClass(ClassIds.RENDERER)).starProjectedType
    }

    override fun visitClass(declaration: IrClass): IrStatement {
      val origin = declaration.origin
      if (
        origin is IrDeclarationOrigin.GeneratedByPlugin &&
          origin.pluginKey == Keys.ContributesRendererGeneratorKey &&
          declaration.name == ContributesRendererIds.NESTED_INTERFACE_NAME
      ) {
        generateFunctions(declaration)
      }
      return super.visitClass(declaration)
    }

    private fun generateFunctions(contributionClass: IrClass) {
      val rendererClass = contributionClass.parentAsClass
      val companion =
        contributionClass.declarations.filterIsInstance<IrClass>().single { it.isCompanion }
      val providerName =
        "provide${ContributesRendererIds.generatedSafeClassNamePrefix(requireNotNull(rendererClass.classId))}"
      if (!rendererClass.hasInjectAnnotation()) {
        generateProvider(
          companion = companion,
          rendererClass = rendererClass,
          name = providerName,
        )
      }

      for (modelClass in rendererClass.modelClasses()) {
        val bindingName =
          providerName +
            ContributesRendererIds.generatedModelClassNameSuffix(
              requireNotNull(modelClass.classId),
            )
        generateBinding(
          contributionClass = contributionClass,
          rendererClass = rendererClass,
          modelClass = modelClass,
          name = bindingName,
        )
        generateKey(
          companion = companion,
          rendererClass = rendererClass,
          modelClass = modelClass,
          name = bindingName + "Key",
        )
      }
    }

    private fun generateProvider(companion: IrClass, rendererClass: IrClass, name: String) {
      val constructor = rendererClass.primaryConstructor ?: rendererClass.constructors.first()
      val function =
        companion.addGeneratedFunction(
          name = name,
          returnType = rendererClass.defaultType,
          modality = Modality.OPEN,
        )
      function.addAnnotation(ClassIds.PROVIDES)
      constructor.parameters
        .filter { it.kind == IrParameterKind.Regular }
        .forEach { parameter ->
          function.parameters += parameter.copyTo(function).apply { defaultValue = null }
        }
      val builder = irBuilderFor(function)
      function.body = builder.irBlockBody {
        val call = irCallConstructor(constructor.symbol, emptyList())
        function.parameters
          .filter { it.kind == IrParameterKind.Regular }
          .forEachIndexed { index, parameter ->
            call.arguments[index] = irGet(parameter)
          }
        +irReturn(call)
      }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun generateBinding(
      contributionClass: IrClass,
      rendererClass: IrClass,
      modelClass: IrClass,
      name: String,
    ) {
      val function =
        contributionClass.addGeneratedFunction(
          name = name,
          returnType = rendererType,
          modality = Modality.ABSTRACT,
        )
      function.addValueParameter(name = "renderer", type = rendererClass.defaultType)
      function.addAnnotation(ClassIds.BINDS)
      function.addAnnotation(ClassIds.INTO_MAP)
      function.addAnnotation(classId = ClassIds.RENDERER_KEY, argument = modelClass.symbol)
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun generateKey(
      companion: IrClass,
      rendererClass: IrClass,
      modelClass: IrClass,
      name: String,
    ) {
      val function =
        companion.addGeneratedFunction(
          name = name,
          returnType =
            pluginContext.irBuiltIns.kClassClass.typeWithArguments(
              listOf(
                makeTypeProjection(
                  type = rendererType,
                  variance = Variance.OUT_VARIANCE,
                ),
              ),
            ),
          modality = Modality.OPEN,
        )
      function.addAnnotation(ClassIds.PROVIDES)
      function.addAnnotation(ClassIds.INTO_MAP)
      function.addAnnotation(classId = ClassIds.RENDERER_KEY, argument = modelClass.symbol)
      function.addAnnotation(
        classId = ClassIds.FOR_SCOPE,
        argument = requireNotNull(pluginContext.referenceClass(ClassIds.RENDERER_SCOPE)),
      )
      function.body =
        irBuilderFor(function).irBlockBody {
          +irReturn(classReference(rendererClass.symbol))
        }
      metadataRegistrar.registerFunctionAsMetadataVisible(function)
    }

    private fun IrClass.addGeneratedFunction(
      name: String,
      returnType: IrType,
      modality: Modality,
    ): IrSimpleFunction {
      val rendererClass = if (isCompanion) parentAsClass.parentAsClass else parentAsClass
      return addFunction {
        this.name = Name.identifier(name)
        this.returnType = returnType
        this.modality = modality
        visibility = DescriptorVisibilities.PUBLIC
        origin = IrDeclarationOrigin.GeneratedByPlugin(Keys.ContributesRendererGeneratorKey)
        startOffset = rendererClass.startOffset
        endOffset = rendererClass.endOffset
      }
        .apply {
          parameters +=
            requireNotNull(this@addGeneratedFunction.thisReceiver).copyTo(this).apply {
              kind = IrParameterKind.DispatchReceiver
            }
        }
    }

    private fun IrSimpleFunction.addAnnotation(classId: ClassId, argument: IrClassSymbol? = null) {
      val annotationClass = requireNotNull(pluginContext.referenceClass(classId))
      val annotation =
        irBuilderFor(this)
          .irAnnotationCompat(
            callee = annotationClass.constructors.single(),
            typeArguments = emptyList(),
          )
      if (argument != null) {
        annotation.arguments[0] = classReference(argument)
      }
      addAnnotationCompat(annotation)
    }

    private fun classReference(classSymbol: IrClassSymbol): IrClassReference =
      IrClassReferenceImpl(
        startOffset = UNDEFINED_OFFSET,
        endOffset = UNDEFINED_OFFSET,
        type = pluginContext.irBuiltIns.kClassClass.typeWith(classSymbol.starProjectedType),
        symbol = classSymbol,
        classType = classSymbol.starProjectedType,
      )

    private fun IrClass.findAnnotation(classId: ClassId): IrConstructorCall? =
      annotationsCompat().firstOrNull { it.symbol.owner.parentAsClass.classId == classId }

    private fun IrClass.hasInjectAnnotation(): Boolean =
      findAnnotation(ClassIds.INJECT) != null ||
        constructors.any { constructor ->
          constructor.annotationsCompat().any {
            it.symbol.owner.parentAsClass.classId == ClassIds.INJECT
          }
        }

    private fun IrClass.modelClasses(): List<IrClass> {
      val annotation = requireNotNull(findAnnotation(ClassIds.CONTRIBUTES_RENDERER))
      val explicitModelClass =
        (annotation.getAnnotationArgumentCompat(Name.identifier("modelType")) as? IrClassReference)
          ?.classType
          ?.classOrNull
          ?.owner
          ?.takeIf { it.classId != ClassIds.UNIT }
      val modelClass = explicitModelClass ?: implicitModelClasses().single()
      val includeSealedSubtypes =
        (annotation.getAnnotationArgumentCompat(Name.identifier("includeSealedSubtypes"))
            as? IrConst)
          ?.value as? Boolean ?: true
      if (!includeSealedSubtypes) {
        return listOf(modelClass)
      }

      val collected = linkedMapOf<ClassId, IrClass>()
      val queue = ArrayDeque<IrClass>()
      queue += modelClass
      while (queue.isNotEmpty()) {
        val candidate = queue.removeFirst()
        if (collected.putIfAbsent(requireNotNull(candidate.classId), candidate) != null) {
          continue
        }
        if (candidate.modality == Modality.SEALED) {
          queue += candidate.sealedSubclasses.map { it.owner }
        }
      }
      return collected.values.toList()
    }

    private fun IrClass.implicitModelClasses(): List<IrClass> {
      val collected = linkedMapOf<ClassId, IrClass>()
      val visited = mutableSetOf<IrType>()
      val queue = ArrayDeque<IrType>()
      queue += superTypes
      while (queue.isNotEmpty()) {
        val type = queue.removeFirst() as? IrSimpleType ?: continue
        if (!visited.add(type)) {
          continue
        }
        type.arguments.filterIsInstance<IrTypeProjection>().forEach { argument ->
          val candidate = argument.type.classOrNull?.owner ?: return@forEach
          if (candidate.classId != ClassIds.BASE_MODEL && candidate.isBaseModelSubtype()) {
            collected.putIfAbsent(requireNotNull(candidate.classId), candidate)
          }
        }
        val superClass = type.classOrNull?.owner ?: continue
        val substitutions =
          superClass.typeParameters
            .zip(type.arguments)
            .mapNotNull { (parameter, argument) ->
              (argument as? IrTypeProjection)?.type?.let { parameter.symbol to it }
            }
            .toMap()
        val substitutor = IrTypeSubstitutor(substitutions)
        queue += superClass.superTypes.map { substitutor.substitute(it) }
      }
      return collected.values.toList()
    }

    private fun IrClass.isBaseModelSubtype(): Boolean {
      val visited = mutableSetOf<IrClass>()
      val queue = ArrayDeque<IrClass>()
      queue += this
      while (queue.isNotEmpty()) {
        val candidate = queue.removeFirst()
        if (!visited.add(candidate)) {
          continue
        }
        if (candidate.classId == ClassIds.BASE_MODEL) {
          return true
        }
        queue += candidate.superTypes.mapNotNull { it.classOrNull?.owner }
      }
      return false
    }

    private fun irBuilderFor(declaration: IrSimpleFunction) =
      DeclarationIrBuilder(pluginContext, declaration.symbol, UNDEFINED_OFFSET, UNDEFINED_OFFSET)
  }
}
