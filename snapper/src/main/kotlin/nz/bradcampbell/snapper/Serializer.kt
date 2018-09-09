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

abstract class Serializer<T> {
  abstract fun write(sink: BufferedSink, value: T)
  abstract fun read(source: BufferedSource): T

  fun nullSafe(): Serializer<T?> {
    val delegate = this
    return object : Serializer<T?>() {
      override fun write(sink: BufferedSink, value: T?) {
        if (value == null) {
          sink.writeByte(0)
        } else {
          sink.writeByte(1)
          delegate.write(sink, value)
        }
      }

      override fun read(source: BufferedSource): T? {
        return if (source.readByte() == 1.toByte()) {
          delegate.read(source)
        } else {
          null
        }
      }
    }
  }

  interface Factory {
    fun create(type: Type, snapper: Snapper): Serializer<*>?
  }
}
