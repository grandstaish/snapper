package nz.bradcampbell.snapper

import okio.BufferedSink
import okio.BufferedSource
import java.util.LinkedHashMap

fun BufferedSink.writeString(value: String) {
  writeInt(value.length)
  writeUtf8(value)
}

fun BufferedSource.readString(): String {
  val length = readInt()
  return readUtf8(length.toLong())
}

fun BufferedSink.writeBoolean(value: Boolean) = writeByte(if (value) 1 else 0)

fun BufferedSource.readBoolean(): Boolean = readByte() == 1.toByte()

fun BufferedSink.writeDouble(value: Double) = writeLongLe(value.toBits())

fun BufferedSource.readDouble(): Double = Double.fromBits(readLongLe())

fun BufferedSink.writeFloat(value: Float) = writeIntLe(value.toBits())

fun BufferedSource.readFloat(): Float = Float.fromBits(readIntLe())

inline fun <T> BufferedSink.writeCollection(
    value: Collection<T>,
    writer: BufferedSink.(T) -> Unit
) {
  writeInt(value.size)
  value.forEach { writer(this, it) }
}

inline fun <T> BufferedSource.readCollection(
    collectionFactory: (Int) -> MutableCollection<T>,
    reader: BufferedSource.() -> T
): Collection<T> {
  val size = readInt()
  val value = collectionFactory(size)
  for (i in 0 until size) {
    value.add(reader())
  }
  return value
}

inline fun <K, V> BufferedSink.writeMap(
    value: Map<K, V>,
    keyWriter: BufferedSink.(K) -> Unit,
    valueWriter: BufferedSink.(V) -> Unit
) {
  writeInt(value.size)
  value.forEach { (k, v) ->
    keyWriter(k)
    valueWriter(v)
  }
}

inline fun <K, V> BufferedSource.readMap(
    keyReader: BufferedSource.() -> K,
    valueReader: BufferedSource.() -> V
): Map<K, V> {
  val size = readInt()
  val map = LinkedHashMap<K, V>(size)
  for (i in 0 until size) {
    map[keyReader()] = valueReader()
  }
  return map
}
