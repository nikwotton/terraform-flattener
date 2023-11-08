package terraform.flattener

import kotlinx.serialization.json.JsonObject

sealed class TerraformType {
    abstract val name: String

    data class Resource(val resourceType: String, override val name: String, val count: String?, val fields: Map<String, String>) : TerraformType(), FinalTerraformTypes
    data class Module(override val name: String, val source: String, val count: String?, val fields: Map<String, String>) : TerraformType()
    data class Variable(override val name: String, val default: String?) : TerraformType()
    data class Local(override val name: String, val value: String) : TerraformType()
    data class Provider(override val name: String, val fields: Map<String, String>) : TerraformType(), FinalTerraformTypes
    data class Terraform(val body: JsonObject) : TerraformType(), FinalTerraformTypes {
        override val name: String = ""
    }
    data class Output(override val name: String, val body: String) : TerraformType(), FinalTerraformTypes
}

sealed interface FinalTerraformTypes
