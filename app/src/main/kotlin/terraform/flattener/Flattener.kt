package terraform.flattener

import java.io.BufferedReader
import java.io.File
import java.io.InputStream
import java.io.InputStreamReader
import java.util.function.Consumer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

val json by lazy { Json { prettyPrint = true } }

fun flatten(inputDir: File, outputFile: File) {
    // Initial Setup
    if (outputFile.exists())
        outputFile.delete()

    val resources = getResources(inputDir)

    val decoded = json.decodeFromString<JsonObject>(resources).toMutableMap()
    // TODO: Modules shouldn't exist at this point...
//    val modules = decoded["module"]?.jsonObject?.entries?.joinToString("\n\n") { """module "${it.key}" ${it.value.jsonArray[0].toResourceStrings().split("\n").joinToString("\n") { "  $it" }.trim()}""" }
    val resourcs = decoded["resource"]?.jsonObject?.entries?.joinToString("\n\n"){(key, value) -> value.jsonObject.entries.joinToString("\n") { """resource "$key" "${it.key}" ${it.value.jsonArray[0].toResourceStrings()}"""}}
    decoded.remove("resource")
//    val local = decoded["locals"]?.jsonArray // TODO: locals should be inlined
//    val provider = decoded["provider"]?.jsonObject // TODO: Translate this
//    val terraform = decoded["terraform"]?.jsonArray // TODO: Translate this
    require(decoded.isEmpty()) { "Found unhandled values in decoded:\n${decoded}" }

//    outputFile.writeText("$terraform\n\n$provider\n\n$resourcs")
    outputFile.writeText(resourcs + "\n")

//    TODO("At the end of what's implemented so far")
}

fun getResources(inputDir: File, inputs: Map<String, JsonPrimitive> = emptyMap()): String {
    // Parsing top level resources
    val resources = inputDir.listFiles().orEmpty().filter { it.isFile && it.extension == "tf" }.map {
        // TODO: Convert this to some sort of gradle-based dependency on hcl2json
        val jsonString = runCommand(it.parentFile, "/opt/homebrew/bin/hcl2json", it.absolutePath)
        val decoded = json.decodeFromString<JsonObject>(jsonString)
        decoded.jsonObject.toMap()
    }.fold(mutableMapOf<String, JsonElement>()) { acc, next ->
        val importantKeys = (acc.keys + next.keys).toSet()
        val ret = mutableMapOf<String, JsonElement?>()
        importantKeys.forEach { ret[it] = acc[it] + next[it] }
        ret.filterNotNull()
    }
    var currentJson = json.encodeToString(JsonObject(resources.toMutableMap()))

    // Handle sources - map relative paths to absolute paths
    currentJson = json.encodeToString(JsonObject(json.decodeFromString<JsonObject>(currentJson).toMutableMap().map {
        if (it.key == "module") {
            it.key to JsonObject(it.value.jsonObject.toMutableMap().map {
                it.key to JsonArray(it.value.jsonArray.map {
                    JsonObject(it.jsonObject.toMutableMap().map {
                        if (it.key == "source") {
                            it.key to JsonPrimitive(inputDir.absolutePath + File.separator + it.value.toString().removeSurrounding("\""))
                        } else
                            it.key to it.value
                    }.toMap().toMutableMap())
                })
            }.toMap().toMutableMap())
        } else it.key to it.value
    }.toMap().toMutableMap()))

    // Handle environment variables
    if (resources.containsKey("variable")) {
        json.decodeFromString<JsonObject>(currentJson).jsonObject["variable"]!!.jsonObject.keys.forEach {
            val value = inputs[it] ?: {
                val environmentKey = "TF_VAR_$it"
                System.getenv(environmentKey)
                    ?: throw IllegalArgumentException("Please set `$environmentKey` as an environment variable or `$it` as an input and re-run")
            }
            val decoded = json.decodeFromString<JsonObject>(currentJson).jsonObject.toMutableMap()
            decoded["variable"] = JsonObject(decoded["variable"]!!.jsonObject.toMutableMap().apply { remove(it) })
            currentJson = json.encodeToString(JsonObject(decoded)).replace("\${var.$it}", value.toString().removeSurrounding("\""))
        }
        currentJson = json.encodeToString(JsonObject(json.decodeFromString<JsonObject>(currentJson).toMutableMap().apply { remove("variable") }))
    }

    // Handle locals
    if (resources.containsKey("locals")) {
        json.decodeFromString<JsonObject>(currentJson).jsonObject["locals"]!!.jsonArray.flatMap { it.jsonObject.toMutableMap().entries }.forEach { (key, value) ->
            val decoded = json.decodeFromString<JsonObject>(currentJson).jsonObject.toMutableMap()
            currentJson = json.encodeToString(JsonObject(decoded)).replace("\"\${local.$key}\"", value.toString().removeSurrounding("\""))
        }
        currentJson = json.encodeToString(JsonObject(json.decodeFromString<JsonObject>(currentJson).toMutableMap().apply { remove("locals") }))
    }

    // Handle modules
    if (resources.containsKey("module")) {
        json.decodeFromString<JsonObject>(currentJson).jsonObject["module"]!!.jsonObject.forEach { (moduleName, moduleArray) ->
            moduleArray.jsonArray.map { it.jsonObject }.forEach { module ->
                val decoded = json.decodeFromString<JsonObject>(currentJson).jsonObject.toMutableMap()
                val moduleSource = File(module["source"]!!.jsonPrimitive.toString().removeSurrounding("\""))
                require(moduleSource.exists() && moduleSource.isDirectory) { "Unable to find module source directory: ${moduleSource.absolutePath}" }
                val translatedModule = json.decodeFromString<JsonObject>(getResources(moduleSource, module.toMutableMap().apply { remove("source") }.map { it.key to it.value.jsonPrimitive }.toMap()))

                decoded.nestedMerge(translatedModule.toMutableMap().map {
                    if (it.key == "resource")
                        it.key to JsonObject(it.value.jsonObject.toMutableMap().map { it.key to JsonObject(it.value.jsonObject.toMutableMap().map { "${moduleName}_${it.key}" to it.value }.toMap().toMutableMap()) }.toMap().toMutableMap())
                    else
                        it.key to it.value
                }.toMap().toMutableMap())
                decoded["module"] = JsonObject(decoded["module"]!!.jsonObject.toMutableMap().map {
                    if (it.key == moduleName) {
                        it.key to JsonArray(it.value.jsonArray.toMutableList().apply { remove(module) })
                    } else {
                        it.key to it.value
                    }
                }.toMap().toMutableMap())
                currentJson = json.encodeToString(JsonObject(decoded))
            }
        }
        currentJson = json.encodeToString(JsonObject(json.decodeFromString<JsonObject>(currentJson).toMutableMap().apply { remove("module") }))
    }


    // TODO: Handle source injection
    // TODO: Handle importing module outputs


    return currentJson
}

fun JsonObject.toResourceStrings(): String = entries.joinToString("\n", "{\n", "\n}") { "  ${if (it.key.contains(".md")) "\"${it.key}\"" else it.key} = ${it.value.toResourceStrings()}" }

fun JsonArray.toResourceStrings(): String = joinToString(", ", "[", "]") { it.toResourceStrings() }
fun JsonElement.toResourceStrings(): String {
    if (isJsonObject()) return jsonObject.toResourceStrings()
    if (isJsonArray()) return jsonArray.toResourceStrings()
    if (isJsonPrimitive()) return jsonPrimitive.toResourceStrings()
    throw IllegalStateException("Found something that isn't an object or array - need to handle it")
}

fun JsonPrimitive.toResourceStrings(): String = when {
    this.isString -> "$this"
    this.isInt() -> "${this.int}"
    this.isBoolean() -> "${this.boolean}"
    this.toString() == "null" -> "null"
    else -> TODO("Another type of primitive - $this")
}

fun MutableMap<String, JsonElement>.nestedMerge(other: MutableMap<String, JsonElement>) {
    other.forEach { (key, value) ->
        if (!containsKey(key)) {
            put(key, value)
        } else {
            when {
                value.isJsonObject() -> {
                    val thisValue = this[key]!!.jsonObject.toMutableMap()
                    val otherValue = value.jsonObject.toMutableMap()
                    otherValue.nestedMerge(thisValue)
                    put(key, JsonObject(otherValue))
                }
                value.isJsonArray() -> TODO("Need to handle arrays apparently in nestedMerge")
                value.isJsonPrimitive() -> TODO("Found a conflict at key `$key` of `$value` and `${this[key]!!.jsonPrimitive}`")
                else -> TODO("Umhandled nestedMerge case")
            }
        }
    }
}

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
