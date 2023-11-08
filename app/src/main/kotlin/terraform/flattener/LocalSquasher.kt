package terraform.flattener

import terraform.flattener.TerraformType.Local
import terraform.flattener.TerraformType.Module
import terraform.flattener.TerraformType.Output
import terraform.flattener.TerraformType.Provider
import terraform.flattener.TerraformType.Resource
import terraform.flattener.TerraformType.Terraform
import terraform.flattener.TerraformType.Variable

fun List<TerraformType>.squashLocals(): List<TerraformType> {
    val toReplace = filterIsInstance<Local>().map {
        it.name to it.value.removeSurrounding("\"\${", "}\"").replace("\\n", "\n").replace("\\\"", "\"")
    }.toMap()
    fun String.replaceVariables(): String {
        var ret = this
        toReplace.forEach { ret = ret.replace("\${local.${it.key}}", it.value.removeSurrounding("\"")).replace("local.${it.key}", it.value) }
        return ret
    }
    return this.filter { it !is Local }.map {
        when (it) {
            is Local -> TODO("dafuq?")
            is Module -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap(), count = it.count?.replaceVariables())
            is Resource -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap(), count = it.count?.replaceVariables())
            is Variable -> it
            is Provider -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap())
            is Terraform -> it
            is Output -> it.copy(body = it.body.replaceVariables())
        }
    }
}
