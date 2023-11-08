package terraform.flattener

import java.io.File
/*
# TODO: NOTE - if flattening, stacktraces WILL be broken....
# Potential fixes:
# - if planning with flatten'ed fails, re-plan without flattening
# - make names as understandable as possible, so stacktraces aren't too bad
# - translate all stacktraces
 */
fun main(args: Array<String>) {
    require(args.isNotEmpty()) { "Please pass in the starting directory you want to flatten and the output file path" }
    require(args.size == 2) { "Please pass only the starting directory you want to flatten and the output file path" }
    val inputDir = File(args.first())
    val outputFile = File(args[1])
    require(inputDir.exists()) { "Path not found, please correct it and try again" }
    require(inputDir.isDirectory) { "Currently only directories are supported not files" }
    require(!inputDir.listFiles().orEmpty().none { it.extension == "tf" }) { "No terraform files found...please check your path and try again" }
    flatten(inputDir, outputFile)
}
