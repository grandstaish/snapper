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
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.ParameterizedType

internal class StandardSerializers private constructor() {
  companion object {
    val FACTORY = object : Serializer.Factory {
      override fun create(type: Type, snapper: Snapper): Serializer<*>? {
        if (type === Unit::class.java) return UNIT_SERIALIZER
        if (type === Boolean::class.javaPrimitiveType) return BOOLEAN_SERIALIZER
        if (type === Byte::class.javaPrimitiveType) return BYTE_SERIALIZER
        if (type === Char::class.javaPrimitiveType) return CHAR_SERIALIZER
        if (type === Double::class.javaPrimitiveType) return DOUBLE_SERIALIZER
        if (type === Float::class.javaPrimitiveType) return FLOAT_SERIALIZER
        if (type === Int::class.javaPrimitiveType) return INT_SERIALIZER
        if (type === Long::class.javaPrimitiveType) return LONG_SERIALIZER
        if (type === Short::class.javaPrimitiveType) return SHORT_SERIALIZER
        if (type === Boolean::class.javaObjectType) return BOOLEAN_SERIALIZER.nullSafe()
        if (type === Byte::class.javaObjectType) return BYTE_SERIALIZER.nullSafe()
        if (type === Char::class.javaObjectType) return CHAR_SERIALIZER.nullSafe()
        if (type === Double::class.javaObjectType) return DOUBLE_SERIALIZER.nullSafe()
        if (type === Float::class.javaObjectType) return FLOAT_SERIALIZER.nullSafe()
        if (type === Int::class.javaObjectType) return INT_SERIALIZER.nullSafe()
        if (type === Long::class.javaObjectType) return LONG_SERIALIZER.nullSafe()
        if (type === Short::class.javaObjectType) return SHORT_SERIALIZER.nullSafe()
        if (type === String::class.java) return STRING_SERIALIZER.nullSafe()

        val rawType = Types.getRawType(type)

        val serializableAnnotation = rawType.getAnnotation(Serializable::class.java)
        if (serializableAnnotation != null) {
          return generatedSerializer(snapper, type, rawType)
        }

        return if (rawType.isEnum) {
          @Suppress("UNCHECKED_CAST")
          EnumSerializer(rawType as Class<Any>).nullSafe()
        } else {
          null
        }
      }
    }

    val UNIT_SERIALIZER = object : Serializer<Unit>() {
      override fun write(sink: BufferedSink, value: Unit) {
      }

      override fun read(source: BufferedSource) {
      }

      override fun toString(): String {
        return "Serializer(Unit)"
      }
    }

    val BOOLEAN_SERIALIZER = object : Serializer<Boolean>() {
      override fun write(sink: BufferedSink, value: Boolean) {
        sink.writeBoolean(value)
      }

      override fun read(source: BufferedSource): Boolean {
        return source.readBoolean()
      }

      override fun toString(): String {
        return "Serializer(Boolean)"
      }
    }

    val BYTE_SERIALIZER = object : Serializer<Byte>() {
      override fun write(sink: BufferedSink, value: Byte) {
        sink.writeByte(value.toInt())
      }

      override fun read(source: BufferedSource): Byte {
        return source.readByte()
      }

      override fun toString(): String {
        return "Serializer(Byte)"
      }
    }

    val CHAR_SERIALIZER = object : Serializer<Char>() {
      override fun write(sink: BufferedSink, value: Char) {
        sink.writeInt(value.toInt())
      }

      override fun read(source: BufferedSource): Char {
        return source.readInt().toChar()
      }

      override fun toString(): String {
        return "Serializer(Char)"
      }
    }

    val DOUBLE_SERIALIZER = object : Serializer<Double>() {
      override fun write(sink: BufferedSink, value: Double) {
        sink.writeDouble(value)
      }

      override fun read(source: BufferedSource): Double {
        return source.readDouble()
      }

      override fun toString(): String {
        return "Serializer(Double)"
      }
    }

    val FLOAT_SERIALIZER = object : Serializer<Float>() {
      override fun write(sink: BufferedSink, value: Float) {
        sink.writeFloat(value)
      }

      override fun read(source: BufferedSource): Float {
        return source.readFloat()
      }

      override fun toString(): String {
        return "Serializer(Float)"
      }
    }

    val INT_SERIALIZER = object : Serializer<Int>() {
      override fun write(sink: BufferedSink, value: Int) {
        sink.writeInt(value)
      }

      override fun read(source: BufferedSource): Int {
        return source.readInt()
      }

      override fun toString(): String {
        return "Serializer(Int)"
      }
    }

    val LONG_SERIALIZER = object : Serializer<Long>() {
      override fun write(sink: BufferedSink, value: Long) {
        sink.writeLong(value)
      }

      override fun read(source: BufferedSource): Long {
        return source.readLong()
      }

      override fun toString(): String {
        return "Serializer(Long)"
      }
    }

    val SHORT_SERIALIZER = object : Serializer<Short>() {
      override fun write(sink: BufferedSink, value: Short) {
        sink.writeShort(value.toInt())
      }

      override fun read(source: BufferedSource): Short {
        return source.readShort()
      }

      override fun toString(): String {
        return "Serializer(Short)"
      }
    }

    val STRING_SERIALIZER = object : Serializer<String>() {
      override fun write(sink: BufferedSink, value: String) {
        sink.writeString(value)
      }

      override fun read(source: BufferedSource): String {
        return source.readString()
      }

      override fun toString(): String {
        return "Serializer(String)"
      }
    }

    @Suppress("UNCHECKED_CAST")
    fun generatedSerializer(snapper: Snapper, type: Type, rawType: Class<*>): Serializer<*> {
      val serializerClassName = rawType.name.replace("$", "_") + "Serializer"
      try {
        val serializerClass = Class.forName(serializerClassName, true,
            rawType.classLoader) as Class<out Serializer<*>>
        return if (type is ParameterizedType) {
          val constructor = serializerClass.getDeclaredConstructor(Snapper::class.java,
              Array<Type>::class.java)
          constructor.isAccessible = true
          constructor.newInstance(snapper, type.actualTypeArguments)
        } else {
          val constructor = serializerClass.getDeclaredConstructor(Snapper::class.java)
          constructor.isAccessible = true
          constructor.newInstance(snapper)
        }
      } catch (e: ClassNotFoundException) {
        throw RuntimeException("Failed to find the generated Serializer class for $rawType", e)
      } catch (e: NoSuchMethodException) {
        throw RuntimeException("Failed to find the generated Serializer constructor for $rawType",
            e)
      } catch (e: IllegalAccessException) {
        throw RuntimeException("Failed to access the generated Serializer for $rawType", e)
      } catch (e: InvocationTargetException) {
        throw RuntimeException("Failed to construct the generated Serializer for $rawType", e)
      } catch (e: InstantiationException) {
        throw RuntimeException("Failed to instantiate the generated Serializer for $rawType", e)
      }
    }
  }

  class EnumSerializer(private val enumType: Class<Any>) : Serializer<Any>() {
    private val constants: Array<Any> = enumType.enumConstants
    private val nameStrings: List<String> = constants.map { (it as Enum<*>).name }

    override fun write(sink: BufferedSink, value: Any) {
      sink.writeString(nameStrings[(value as Enum<*>).ordinal])
    }

    override fun read(source: BufferedSource): Any {
      return constants[nameStrings.indexOf(source.readString())]
    }

    override fun toString(): String {
      return "Serializer(" + enumType.name + ")"
    }
  }
}
