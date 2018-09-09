package nz.bradcampbell.snapper.compiler

import org.assertj.core.api.Assertions.assertThat
import org.jetbrains.kotlin.cli.common.ExitCode
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import javax.annotation.processing.Processor

class SerializerProcessorTest {
  @Rule
  @JvmField
  var temporaryFolder: TemporaryFolder = TemporaryFolder()

  @Test
  fun privateConstructor() {
    val call = KotlinCompilerCall(temporaryFolder.root)
    call.inheritClasspath = true
    call.addService(Processor::class, SerializableProcessor::class)
    call.addKt("source.kt", """
        |import nz.bradcampbell.snapper.Serializable
        |
        |@Serializable
        |class PrivateConstructor private constructor(var a: Int, var b: Int) {
        |  fun a() = a
        |  fun b() = b
        |  companion object {
        |    fun newInstance(a: Int, b: Int) = PrivateConstructor(a, b)
        |  }
        |}
        |""".trimMargin())

    val result = call.execute()
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.systemErr).contains("constructor is not internal or public")
  }

  @Test
  fun privateClass() {
    val call = KotlinCompilerCall(temporaryFolder.root)
    call.inheritClasspath = true
    call.addService(Processor::class, SerializableProcessor::class)
    call.addKt("source.kt", """
        |import nz.bradcampbell.snapper.Serializable
        |
        |@Serializable
        |private class PrivateClass(var a: Int, var b: Int) {
        |  fun a() = a
        |  fun b() = b
        |}
        |""".trimMargin())

    val result = call.execute()
    assertThat(result.exitCode).isEqualTo(ExitCode.COMPILATION_ERROR)
    assertThat(result.systemErr).contains("Classes annotated with Serializable must be public")
  }
}
