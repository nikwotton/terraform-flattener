package terraform.flattener

import java.io.File
import terraform.flattener.TerraformType.Module
import terraform.flattener.TerraformType.Output
import terraform.flattener.TerraformType.Provider
import terraform.flattener.TerraformType.Resource
import terraform.flattener.TerraformType.Terraform

fun List<TerraformType>.squashModules(): List<FinalTerraformTypes> {
    val toReplace = HashMap<String, String>()
    val terraforms = ArrayList<Terraform>()
    val newResources = filterIsInstance<Module>().flatMap { module ->
        val moduleSource = File(module.source)
        require(moduleSource.exists() && moduleSource.isDirectory) { "Unable to find module source directory: ${moduleSource.absolutePath}" }
        val subThings = getResources(moduleSource, module.fields)
        val subResources = subThings.filterIsInstance<Resource>().map { it.copy(name = "${module.name}_${it.name}") }
        val subOutputs = subThings.filterIsInstance<Output>()
        val subTerraforms = subThings.filterIsInstance<Terraform>()
        terraforms.addAll(subTerraforms)
        require((subThings.filter { it !is Resource && it !is Output && it !is Terraform }).isEmpty()) { "Found an unhandled type - ${subThings.filter { it !is Resource && it !is Output && it !is Terraform }.map { it.javaClass.simpleName }}" }
        subOutputs.forEach {
            val key = "module.${module.name}.${it.name}"
            require(!toReplace.containsKey(key)) { "Found duplicate key somehow: $key" }
            toReplace[key] = it.body
        }
        if (module.count != null) {
            subResources.map {
                if (it.count == null) {
                    it.copy(count = module.count)
                } else {
                    val count = "${it.count} * ${module.count}"
                    it.copy(count = count)
                }
            }
        } else subResources
    }
    require(this.all { it is Module || it is FinalTerraformTypes }) { "Found an unhandled type - ${this.filter { it !is FinalTerraformTypes && it !is Module }.map { it.javaClass.simpleName }}" }
    fun String.replaceOutputs(): String {
        var ret = this
        toReplace.forEach { ret = ret.replace(it.key, it.value) }
        return ret
    }
    return filterIsInstance<FinalTerraformTypes>().map {
        when(it) {
            is Resource -> it.copy(fields = it.fields.map { it.key to it.value.replaceOutputs() }.toMap(), count = it.count?.replaceOutputs())
            is Output -> it.copy(body = it.body.replaceOutputs())
            is Provider -> it.copy(fields = it.fields.map { it.key to it.value.replaceOutputs() }.toMap())
            is Terraform -> it
        }
    } + newResources + terraforms
}
