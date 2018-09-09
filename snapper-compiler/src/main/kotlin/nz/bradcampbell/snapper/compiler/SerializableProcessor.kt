package nz.bradcampbell.snapper.compiler

import com.google.auto.common.Visibility
import com.google.auto.service.AutoService
import com.squareup.kotlinpoet.TypeSpec
import me.eugeniomarletti.kotlin.metadata.KotlinMetadataUtils
import me.eugeniomarletti.kotlin.metadata.declaresDefaultValue
import me.eugeniomarletti.kotlin.processing.KotlinAbstractProcessor
import nz.bradcampbell.snapper.Serializable
import java.io.File
import javax.annotation.processing.Processor
import javax.annotation.processing.RoundEnvironment
import javax.lang.model.SourceVersion
import javax.lang.model.element.Element
import javax.lang.model.element.TypeElement
import javax.tools.Diagnostic
import javax.tools.Diagnostic.Kind.ERROR

@AutoService(Processor::class)
class SerializableProcessor : KotlinAbstractProcessor(), KotlinMetadataUtils {
  private val annotation = Serializable::class.java

  override fun getSupportedAnnotationTypes() = setOf(annotation.canonicalName)

  override fun getSupportedSourceVersion(): SourceVersion = SourceVersion.latest()

  override fun process(annotations: Set<TypeElement>, roundEnv: RoundEnvironment): Boolean {
    for (type in roundEnv.getElementsAnnotatedWith(annotation)) {
      val effectiveVisibility = Visibility.effectiveVisibilityOfElement(type)
      if (effectiveVisibility != Visibility.PUBLIC) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Classes annotated with Serializable must be public", type)
        continue
      }
      serializerGenerator(type)?.generateAndWrite()
    }
    return true
  }

  private fun serializerGenerator(element: Element): SerializerGenerator? {
    val type = TargetType.get(messager, elementUtils, typeUtils, element) ?: return null

    val properties = mutableMapOf<String, PropertyGenerator>()
    for (property in type.properties.values) {
      val generator = property.generator(messager)
      if (generator != null) {
        properties[property.name] = generator
      }
    }

    for ((name, parameter) in type.constructor.parameters) {
      if (type.properties[parameter.name] == null && !parameter.proto.declaresDefaultValue) {
        messager.printMessage(ERROR, "No property for required constructor parameter $name",
            parameter.element)
        return null
      }
    }

    // Sort properties so that those with constructor parameters come first.
    val sortedProperties = properties.values.sortedBy {
      if (it.hasConstructorParameter) {
        it.target.parameterIndex
      } else {
        Integer.MAX_VALUE
      }
    }

    return SerializerGenerator(type, sortedProperties)
  }

  private fun SerializerGenerator.generateAndWrite() {
    val fileSpec = generateFile()
    val serializerName = fileSpec.members.filterIsInstance<TypeSpec>().first().name!!
    val outputDir = generatedDir ?: mavenGeneratedDir(serializerName)
    fileSpec.writeTo(outputDir)
  }

  private fun mavenGeneratedDir(serializerName: String): File {
    // Hack since the maven plugin doesn't supply `kapt.kotlin.generated` option
    // Bug filed at https://youtrack.jetbrains.com/issue/KT-22783
    val file = filer.createSourceFile(serializerName).toUri().let(::File)
    return file.parentFile.also { file.delete() }
  }
}
