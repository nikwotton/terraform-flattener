package terraform.flattener

import java.io.File
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import terraform.flattener.TerraformType.Local
import terraform.flattener.TerraformType.Module
import terraform.flattener.TerraformType.Output
import terraform.flattener.TerraformType.Provider
import terraform.flattener.TerraformType.Resource
import terraform.flattener.TerraformType.Terraform
import terraform.flattener.TerraformType.Variable

fun JsonObject.toClasses(inputDir: File): List<TerraformType> = flatMap {
    when (it.key) {
        "module" -> {
            it.value.jsonObject.map { (moduleName, modules) ->
                require(modules.jsonArray.size == 1) { "Found more than one in module list??" }
                val fields = modules.jsonArray.first().jsonObject.map { it.key to it.value.toString().removeSurrounding("\"\${", "}\"").replace("\\n", "\n").replace("\\\"", "\"") }.toMap().toMutableMap()
                val source = inputDir.absolutePath + File.separator + fields["source"]!!.removeSurrounding("\"")
                var count = fields["count"]
                val forEach = fields["for_each"]
                require(count == null || forEach == null) { "Found a resource with a for_each and a count" }
                if (forEach != null) {
                    val startingArray = forEach.removeSurrounding("\"\${", "}\"")
                    count = "length($startingArray)"
                    val wrappedKeys = "can(tolist($startingArray)) ? $startingArray[count.index] : keys($startingArray)[count.index]"
                    val wrappedValues = "can(tolist($startingArray)) ? $startingArray[count.index] : values($startingArray)[count.index]"
                    fields.keys.forEach {
                        fields[it] = fields[it]!!.replace("each.value", wrappedValues).replace("each.key", wrappedKeys)
                    }
                }
                if (count != null) {
                    fields.keys.filter { it !in listOf("for_each", "count") }.filter { fields[it]!!.contains("count.index") }.forEach {
                        // This whole section definitely leaves open plenty of breaking cases....hoping to just not hit those for now
                        val starting = fields[it]!!
                        require(count != null && count!!.startsWith("length(") && count!!.endsWith(")")) { "Haven't figured out how to handle this case yet..." }
                        val list = count!!.removeSurrounding("length(", ")")
                        require(starting.contains("$list[count.index]")) { "Haven't figured out how to handle this case yet..." }
                        fields[it] = starting.replace("$list[count.index]", "$list[floor(count.index % length($list))]")
                            .replace("keys($list)[count.index]", "keys($list)[floor(count.index % length($list))]")
                            .replace("values($list)[count.index]", "values($list)[floor(count.index % length($list))]")
                    }
                }
                Module(name = moduleName, source = source, fields = fields.filterKeys { it !in listOf("source", "for_each", "count") }, count = count)
            }
        }

        "resource" -> {
            it.value.jsonObject.flatMap { (resourceType, resources) ->
                resources.jsonObject.flatMap { (resourceName, resourceArray) ->
                    resourceArray.jsonArray.map { resource ->
                        val fields = resource.jsonObject.map { it.key to it.value.toString().removeSurrounding("\"\${", "}\"").replace("\\n", "\n").replace("\\\"", "\"") }.toMap().toMutableMap()
                        var count = fields["count"]
                        val forEach = fields["for_each"]
                        require(count == null || forEach == null) { "Found a resource with a for_each and a count" }
                        if (forEach != null) {
                            val startingArray = forEach.removeSurrounding("\"\${", "}\"")
                            count = "length($startingArray)"
                            val wrappedKeys = "can(tolist($startingArray)) ? $startingArray[count.index] : keys($startingArray)[count.index]"
                            val wrappedValues = "can(tolist($startingArray)) ? $startingArray[count.index] : values($startingArray)[count.index]"
                            fields.keys.forEach {
                                fields[it] = fields[it]!!.replace("each.value", wrappedValues).replace("each.key", wrappedKeys)
                            }
                        }
                        if (count != null) {
                            fields.keys.filter { it !in listOf("for_each", "count") }.filter { fields[it]!!.contains("count.index") }.forEach {
                                // This whole section definitely leaves open plenty of breaking cases....hoping to just not hit those for now
                                val starting = fields[it]!!
                                require(count != null && count!!.startsWith("length(") && count!!.endsWith(")")) { "Haven't figured out how to handle this case yet..." }
                                val list = count!!.removeSurrounding("length(", ")")
                                require(starting.contains("$list[count.index]")) { "Haven't figured out how to handle this case yet..." }
                                fields[it] = starting.replace("$list[count.index]", "$list[floor(count.index % length($list))]")
                                    .replace("keys($list)[count.index]", "keys($list)[floor(count.index % length($list))]")
                                    .replace("values($list)[count.index]", "values($list)[floor(count.index % length($list))]")
                            }
                        }
                        Resource(resourceType = resourceType, name = resourceName, fields = fields.filterKeys { it !in listOf("for_each", "count") }, count = count)
                    }
                }
            }
        }

        "variable" -> {
            it.value.jsonObject.map { (name, fields) ->
                require(fields.jsonArray.size == 1)
                Variable(name, fields.jsonArray.first().jsonObject["default"]?.toString())
            }
        }

        "locals" -> {
            it.value.jsonArray.flatMap {
                it.jsonObject.map { (name, value) ->
                    Local(name = name, value = value.toString())
                }
            }
        }

        "provider" -> {
            it.value.jsonObject.flatMap { (providerName, fields) ->
                fields.jsonArray.map {
                    Provider(name = providerName, fields = it.jsonObject.map { it.key to it.value.toString() }.toMap())
                }
            }
        }

        "terraform" -> {
            it.value.jsonArray.map {
                Terraform(it.jsonObject)
            }
        }

        "output" -> {
            it.value.jsonObject.entries.flatMap { (name, fieldsList) ->
                fieldsList.jsonArray.map {
                    Output(name, it.jsonObject["value"].toString().removeSurrounding("\"\${", "}\"").replace("\\n", "\n").replace("\\\"", "\""))
                }
            }
        }

        else -> TODO("toClasses else case - ${it.key}")
    }
}
