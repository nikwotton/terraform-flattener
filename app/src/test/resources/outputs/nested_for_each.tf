resource "github_repository" "my_module_my_repo" {
  count = length(["repo1","repo2"]) * length(["prefix1_","prefix2_"])
  name = join("", [["prefix1_","prefix2_"][floor(count.index % length(["prefix1_","prefix2_"]))], ["repo1","repo2"][floor(count.index % length(["repo1","repo2"]))]])
}
