package terraform.flattener

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer
import kotlinx.serialization.json.Json
import terraform.flattener.TerraformType.Provider
import terraform.flattener.TerraformType.Resource
import terraform.flattener.TerraformType.Terraform

val json by lazy { Json { prettyPrint = true } }

fun flatten(inputDir: File, outputFile: File) {
    // Initial Setup
    if (outputFile.exists())
        outputFile.delete()

    val resources = getResources(inputDir)

    val resourcs = resources.filterIsInstance<Resource>().joinToString("\n") {
        """resource "${it.resourceType}" "${it.name}" {${it.count?.let { "\n  count = ${it.removeSurrounding("\"\${'$'}{", "}\"").replace("\\n", "\n").replace("\\\"", "\"")}" } ?: ""}
            |${it.fields.entries.joinToString("\n") { "  ${it.key} = ${it.value.replace("\"\${each.value}\"", "each.value")}" } }
            |}
        """.trimMargin()
    }
    val providers = resources.filterIsInstance<Provider>().joinToString("\n") {
        """provider "${it.name}" {
            |${it.fields.entries.joinToString("\n") { "  ${it.key} = ${it.value.replace("\"\${each.value}\"", "each.value")}" } }
            |}
        """.trimMargin()
    }
    val terraform = resources.filterIsInstance<Terraform>().joinToString("\n") {
        """terraform ${it.body.toResourceStrings()}""".trimMargin()
    }
    require((resources - resources.filterIsInstance<Resource>().toSet() - resources.filterIsInstance<Provider>().toSet() - resources.filterIsInstance<Terraform>().toSet()).isEmpty()) { "Found unhandled thing" }

    var ret = ""
    if (resourcs.isNotBlank()) ret += resourcs + "\n"
    if (providers.isNotBlank()) ret += providers + "\n"
    if (terraform.isNotBlank()) ret += terraform + "\n"
    outputFile.writeText(ret)
}

fun getResources(inputDir: File, inputs: Map<String, String> = emptyMap()): List<FinalTerraformTypes> =
    convertToJson(inputDir).toClasses(inputDir).squashVariables(inputs).squashLocals().squashModules()

// TODO: Delete this:
internal class StreamGobbler(private val inputStream: InputStream, private val consumeInputLine: Consumer<String>) : Thread() {
    override fun run() =
        BufferedReader(InputStreamReader(inputStream)).lines().forEach(consumeInputLine)
}
internal open class StringBuilderConsumer : Consumer<String> {
    private val sb = StringBuilder()

    override fun accept(t: String) {
        sb.appendLine(t)
    }

    override fun toString(): String = sb.toString()
}

// TODO: Delete this
fun runCommand(runDir: File, vararg command: String): String {
    val output = StringBuilder()
    val ret = StringBuilderConsumer()
    val procBuilder = ProcessBuilder(*command).directory(runDir)
    val proc = procBuilder.start()
    val stdoutGobbler = StreamGobbler(proc.inputStream, ret).apply { start() }
    val stderrGobbler = StreamGobbler(proc.errorStream, ret).apply { start() }
    val exitVal = proc.waitFor()
    stderrGobbler.join()
    stdoutGobbler.join()
    if (exitVal != 0) {
        throw IllegalStateException(ret.toString())
    }
    output.appendLine(ret.toString())
    return output.toString()
}
