module "my_module" {
  for_each = ["prefix1_", "prefix2_"]
  source = "./my_module"
  prefix = each.value
}
// name -> ["prefix1_","prefix2_"][count.index] + ["repo1","repo2"][count.index]
// name -> ["prefix1_","prefix2_"][count.index/length([])] + ["repo1","repo2"][count.index%length([])]
// name -> join("", [["p1_","p2_","p3_"][count.index],          ["r1","r2","r3","r4"][count.index]])
// name -> join("", [["p1_","p2_","p3_"][floor(count.index/2)], ["r1","r2","r3","r4"][floor(count.index%2)]])
