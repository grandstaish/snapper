package nz.bradcampbell.snapper

fun Int.Companion.serializer() = StandardSerializers.INT_SERIALIZER
fun String.Companion.serializer() = StandardSerializers.STRING_SERIALIZER
fun Long.Companion.serializer() = StandardSerializers.LONG_SERIALIZER
fun Short.Companion.serializer() = StandardSerializers.SHORT_SERIALIZER
fun Float.Companion.serializer() = StandardSerializers.FLOAT_SERIALIZER
fun Double.Companion.serializer() = StandardSerializers.DOUBLE_SERIALIZER
fun Char.Companion.serializer() = StandardSerializers.CHAR_SERIALIZER
fun Unit.serializer() = StandardSerializers.UNIT_SERIALIZER
