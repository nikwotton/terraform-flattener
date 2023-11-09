resource "github_repository" "my_module_my_repo" {
  count = length(["repo1","repo2"]) * length(["prefix1_","prefix2_"])
  name = join("", [can(tolist(["prefix1_","prefix2_"])) ? ["prefix1_","prefix2_"][floor(count.index % length(["prefix1_","prefix2_"]))] : values(["prefix1_","prefix2_"])[floor(count.index % length(["prefix1_","prefix2_"]))], can(tolist(["repo1","repo2"])) ? ["repo1","repo2"][floor(count.index % length(["repo1","repo2"]))] : values(["repo1","repo2"])[floor(count.index % length(["repo1","repo2"]))]])
}
