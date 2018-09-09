/*
 * Copyright (C) 2018 Square, Inc.
 * Modification (C) 2018 Bradley Campbell
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nz.bradcampbell.snapper.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.NameAllocator
import com.squareup.kotlinpoet.ParameterSpec
import com.squareup.kotlinpoet.ParameterizedTypeName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.TypeVariableName
import com.squareup.kotlinpoet.WildcardTypeName
import com.squareup.kotlinpoet.asClassName
import nz.bradcampbell.snapper.Serializer

/** A serializer that can be used to encode and decode a particular field. */
internal data class DelegateKey(private val type: TypeName) {
  val nullable get() = type.nullable

  /** Returns a serializer to use when encoding and decoding this property. */
  fun generateProperty(
      nameAllocator: NameAllocator,
      typeRenderer: TypeRenderer,
      snapperParameter: ParameterSpec
  ): PropertySpec {
    val serializerName = nameAllocator.newName("${type.toVariableName().decapitalize()}Serializer",
        this)

    val serializerTypeName = Serializer::class.asClassName().parameterizedBy(type)
    val args = arrayOf(
        snapperParameter,
        if (type is ClassName) "" else CodeBlock.of("<%T>", type),
        typeRenderer.render(type)
    )

    val nullModifier = if (nullable) ".nullSafe()" else ""

    return PropertySpec.builder(serializerName, serializerTypeName, KModifier.PRIVATE)
        .initializer("%1N.serializer%2L(%3L)$nullModifier", *args)
        .build()
  }
}

/**
 * Returns a suggested variable name derived from a list of type names. This just concatenates,
 * yielding types like MapOfStringLong.
 */
private fun List<TypeName>.toVariableNames() = joinToString("") { it.toVariableName() }

/** Returns a suggested variable name derived from a type name, like nullableListOfString. */
private fun TypeName.toVariableName(): String {
  val base = when (this) {
    is ClassName -> simpleName
    is ParameterizedTypeName -> rawType.simpleName + "Of" + typeArguments.toVariableNames()
    is WildcardTypeName -> (lowerBounds + upperBounds).toVariableNames()
    is TypeVariableName -> name + bounds.toVariableNames()
    else -> throw IllegalArgumentException("Unrecognized type! $this")
  }

  return if (nullable) {
    "Nullable$base"
  } else {
    base
  }
}
