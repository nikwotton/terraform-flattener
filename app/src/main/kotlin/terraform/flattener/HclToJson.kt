package terraform.flattener

import java.io.File
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject

fun convertToJson(inputDir: File): JsonObject {
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
    return JsonObject(resources.toMutableMap())
}
