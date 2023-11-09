package terraform.flattener

import terraform.flattener.TerraformType.Local
import terraform.flattener.TerraformType.Module
import terraform.flattener.TerraformType.Output
import terraform.flattener.TerraformType.Provider
import terraform.flattener.TerraformType.Resource
import terraform.flattener.TerraformType.Terraform
import terraform.flattener.TerraformType.Variable


fun List<TerraformType>.squashVariables(inputs: Map<String, String>): List<TerraformType> {
    val toReplace = filterIsInstance<Variable>().associate {
        val value = inputs[it.name] ?: it.default ?: run {
            val environmentKey = "TF_VAR_${it.name}"
            System.getenv(environmentKey)
                ?: throw IllegalArgumentException("Please set `$environmentKey` as an environment variable or `${it.name}` as an input and re-run")
        }
        it.name to value
    }

    fun String.replaceVariables(): String {
        var ret = this
        toReplace.forEach { ret = ret.replace("\${var.${it.key}}", it.value).replace("var.${it.key}", it.value) }
        return ret
    }
    return this.filter { it !is Variable }.map {
        when(it) {
            is Resource -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap(), count = it.count?.replaceVariables())
            is Module -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap(), count = it.count?.replaceVariables())
            is Variable -> TODO("dafuq??")
            is Local -> it
            is Provider -> it.copy(fields = it.fields.map { it.key to it.value.replaceVariables() }.toMap())
            is Terraform -> it
            is Output -> it.copy(body = it.body.replaceVariables())
        }
    }
}
