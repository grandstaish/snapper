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

internal class MapSerializer<K, V>(
    private val keySerializer: Serializer<K>,
    private val valueSerializer: Serializer<V>
) : Serializer<Map<K, V>>() {
  companion object {
    val FACTORY = object : Serializer.Factory {
      override fun create(type: Type, snapper: Snapper): Serializer<*>? {
        val rawType = Types.getRawType(type)
        if (rawType !== Map::class.java) return null
        val keyAndValue = Types.mapKeyAndValueTypes(type, rawType)
        val keySerializer = snapper.serializer<Any>(keyAndValue[0])
        val valueSerializer = snapper.serializer<Any>(keyAndValue[1])
        return MapSerializer(keySerializer, valueSerializer).nullSafe()
      }
    }
  }

  override fun write(sink: BufferedSink, value: Map<K, V>) {
    sink.writeMap(
        value = value,
        keyWriter = { keySerializer.write(this, it) },
        valueWriter = { valueSerializer.write(this, it) }
    )
  }

  override fun read(source: BufferedSource): Map<K, V> {
    return source.readMap(
        keyReader = { keySerializer.read(this) },
        valueReader = { valueSerializer.read(this) }
    )
  }

  override fun toString(): String {
    return "MapSerializer($keySerializer=$valueSerializer)"
  }
}
