/*
 * Copyright (C) 2014 Square, Inc.
 * Modifications (C) 2018 Bradley Campbell
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
package nz.bradcampbell.snapper

import okio.BufferedSink
import okio.BufferedSource
import java.lang.reflect.Type

internal class ArraySerializer(
    private val elementClass: Class<*>,
    private val elementSerializer: Serializer<Any>
) : Serializer<Any>() {
  companion object {
    val FACTORY = object : Serializer.Factory {
      override fun create(type: Type, snapper: Snapper): Serializer<*>? {
        val elementType = Types.arrayComponentType(type) ?: return null
        val elementClass = Types.getRawType(elementType)
        val elementSerializer = snapper.serializer<Any>(elementType)
        return ArraySerializer(elementClass, elementSerializer).nullSafe()
      }
    }
  }

  override fun write(sink: BufferedSink, value: Any) {
    val size = java.lang.reflect.Array.getLength(value)
    sink.writeInt(size)
    for (i in 0 until size) {
      elementSerializer.write(sink, java.lang.reflect.Array.get(value, i))
    }
  }

  override fun read(source: BufferedSource): Any {
    val size = source.readInt()
    val list = mutableListOf<Any?>()
    for (i in 0 until size) {
      list.add(elementSerializer.read(source))
    }
    val array = java.lang.reflect.Array.newInstance(elementClass, size)
    for (i in 0 until list.size) {
      java.lang.reflect.Array.set(array, i, list[i])
    }
    return array
  }

  override fun toString(): String {
    return "$elementSerializer.array()"
  }
}
