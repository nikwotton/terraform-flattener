package terraform.flattener

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive


fun JsonElement.isJsonObject(): Boolean = try {
    jsonObject
    true
} catch (e: IllegalArgumentException) {
    false
}

fun JsonElement.isJsonArray(): Boolean = try {
    jsonArray
    true
} catch (e: IllegalArgumentException) {
    false
}

fun JsonElement.isJsonPrimitive(): Boolean = try {
    jsonPrimitive
    true
} catch (e: IllegalArgumentException) {
    false
}

fun JsonPrimitive.isInt(): Boolean = try {
    int
    true
} catch (e: IllegalArgumentException) {
    false
}

fun JsonPrimitive.isBoolean(): Boolean = try {
    boolean
    true
} catch (e: IllegalStateException) {
    false
}

operator fun JsonElement?.plus(other: JsonElement?): JsonElement? {
    if (this == null) return other
    if (other == null) return this
    return if (this is JsonObject && other is JsonObject)
        JsonObject(this.toMap() + other.toMap())
    else if (this is JsonArray && other is JsonArray)
        JsonArray(this.toList() + other.toList())
    else TODO("Unknown how to handle this")
}

fun <K, V> MutableMap<K, V?>.filterNotNull(): MutableMap<K, V> = entries.filter { it.value != null }.associate { it.key to it.value as V }.toMutableMap()
operator fun JsonObject.minus(key: String): JsonObject = JsonObject(this.toMutableMap().apply { remove(key) })
