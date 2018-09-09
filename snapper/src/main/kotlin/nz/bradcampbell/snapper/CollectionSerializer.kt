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
import java.util.LinkedHashSet
import java.util.ArrayList

internal abstract class CollectionSerializer<C : MutableCollection<T>, T>(
    private val elementSerializer: Serializer<T>
) : Serializer<C>() {
  companion object {
    val FACTORY = object : Serializer.Factory {
      override fun create(type: Type, snapper: Snapper): Serializer<*>? {
        val rawType = Types.getRawType(type)
        if (rawType == List::class.java || rawType == Collection::class.java) {
          return newArrayListSerializer(type, snapper).nullSafe()
        } else if (rawType == Set::class.java) {
          return newLinkedHashSetSerializer(type, snapper).nullSafe()
        }
        return null
      }
    }

    fun newArrayListSerializer(type: Type, snapper: Snapper): Serializer<MutableCollection<Any>> {
      val elementType = Types.collectionElementType(type, Collection::class.java)
      val elementSerializer = snapper.serializer<Any>(elementType)
      return object : CollectionSerializer<MutableCollection<Any>, Any>(elementSerializer) {
        override fun newCollection(size: Int): MutableCollection<Any> {
          return ArrayList(size)
        }
      }
    }

    fun newLinkedHashSetSerializer(type: Type, snapper: Snapper): Serializer<MutableSet<Any>> {
      val elementType = Types.collectionElementType(type, Collection::class.java)
      val elementSerializer = snapper.serializer<Any>(elementType)
      return object : CollectionSerializer<MutableSet<Any>, Any>(elementSerializer) {
        override fun newCollection(size: Int): MutableSet<Any> {
          return LinkedHashSet(size)
        }
      }
    }
  }

  abstract fun newCollection(size: Int): C

  override fun write(sink: BufferedSink, value: C) {
    sink.writeCollection(value) { elementSerializer.write(this, it) }
  }

  @Suppress("UNCHECKED_CAST")
  override fun read(source: BufferedSource): C {
    return source.readCollection(::newCollection) { elementSerializer.read(this) } as C
  }

  override fun toString(): String {
    return "CollectionSerializer($elementSerializer)"
  }
}
