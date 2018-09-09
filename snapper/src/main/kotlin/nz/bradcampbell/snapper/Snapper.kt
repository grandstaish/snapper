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
@file:Suppress("MemberVisibilityCanBePrivate", "unused")

package nz.bradcampbell.snapper

import nz.bradcampbell.snapper.internal.Util
import nz.bradcampbell.snapper.internal.Util.canonicalize
import okio.BufferedSink
import okio.BufferedSource
import java.lang.reflect.Type
import java.util.LinkedHashMap

class Snapper private constructor(builder: Builder) {
  companion object {
    private val BUILT_IN_FACTORIES = listOf(
        StandardSerializers.FACTORY,
        CollectionSerializer.FACTORY,
        MapSerializer.FACTORY,
        ArraySerializer.FACTORY
    )

    private class DeferredSerializer<T>(var cacheKey: Any?) : Serializer<T>() {
      private var delegate: Serializer<T>? = null

      fun ready(delegate: Serializer<T>) {
        this.delegate = delegate
        this.cacheKey = null
      }

      override fun write(sink: BufferedSink, value: T) {
        delegate?.write(sink, value)
            ?: throw IllegalStateException("Serializer isn't ready")
      }

      override fun read(source: BufferedSource): T {
        return delegate?.read(source)
            ?: throw IllegalStateException("Serializer isn't ready")
      }
    }
  }

  private val factories: List<Serializer.Factory>
  private val reentrantCalls = ThreadLocal<MutableList<DeferredSerializer<*>>>()
  private val serializerCache = LinkedHashMap<Type, Serializer<*>>()

  init {
    val factories = mutableListOf<Serializer.Factory>()
    factories.addAll(builder.factories)
    factories.addAll(BUILT_IN_FACTORIES)
    this.factories = factories
  }

  fun <T> serializer(type: Class<T>): Serializer<T> {
    return serializer(type as Type)
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> serializer(type: Type): Serializer<T> {
    val canonicalizedType = canonicalize(type)

    synchronized(serializerCache) {
      val result = serializerCache[canonicalizedType]
      if (result != null) return result as Serializer<T>
    }

    // Short-circuit if this is a reentrant call.
    var deferredSerializers = reentrantCalls.get()
    if (deferredSerializers != null) {
      var i = 0
      val size = deferredSerializers.size
      while (i < size) {
        val deferredSerializer = deferredSerializers[i]
        if (deferredSerializer.cacheKey == canonicalizedType) {
          return deferredSerializer as Serializer<T>
        }
        i++
      }
    } else {
      deferredSerializers = mutableListOf()
      reentrantCalls.set(deferredSerializers)
    }

    // Prepare for re-entrant calls, then ask each factory to create a serializer.
    val deferredSerializer = DeferredSerializer<Any>(canonicalizedType)
    deferredSerializers.add(deferredSerializer)
    try {
      var i = 0
      val size = factories.size
      while (i < size) {
        val result = factories[i].create(canonicalizedType, this) as Serializer<T>?
        if (result != null) {
          deferredSerializer.ready(result as Serializer<Any>)
          synchronized(serializerCache) {
            serializerCache.put(canonicalizedType, result)
          }
          return result
        }
        i++
      }
    } finally {
      deferredSerializers.removeAt(deferredSerializers.size - 1)
      if (deferredSerializers.isEmpty()) {
        reentrantCalls.remove()
      }
    }

    throw IllegalArgumentException("No Serializer for $canonicalizedType")
  }

  @Suppress("UNCHECKED_CAST")
  fun <T> nextSerializer(skipPast: Serializer.Factory, type: Type): Serializer<T> {
    val canonicalizedType = canonicalize(type)

    val skipPastIndex = factories.indexOf(skipPast)
    if (skipPastIndex == -1) {
      throw IllegalArgumentException("Unable to skip past unknown factory $skipPast")
    }
    var i = skipPastIndex + 1
    val size = factories.size
    while (i < size) {
      val result = factories[i].create(canonicalizedType, this) as Serializer<T>?
      if (result != null) return result
      i++
    }

    throw IllegalArgumentException("No Serializer for $canonicalizedType")
  }

  fun newBuilder(): Snapper.Builder {
    val fullSize = factories.size
    val tailSize = BUILT_IN_FACTORIES.size
    val customFactories = factories.subList(0, fullSize - tailSize)
    return Builder().addAll(customFactories)
  }

  class Builder {
    internal val factories = mutableListOf<Serializer.Factory>()

    fun <T> add(type: Type, serializer: Serializer<T>): Builder {
      val targetType = type
      return add(object : Serializer.Factory {
        override fun create(type: Type, snapper: Snapper): Serializer<*>? {
          return if (Util.typesMatch(type, targetType)) serializer else null
        }
      })
    }

    fun add(factory: Serializer.Factory): Builder {
      factories.add(factory)
      return this
    }

    internal fun addAll(factories: List<Serializer.Factory>): Builder {
      this.factories.addAll(factories)
      return this
    }

    fun build(): Snapper {
      return Snapper(this)
    }
  }
}
