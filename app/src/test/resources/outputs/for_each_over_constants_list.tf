resource "github_repository" "my_repo" {
  count = length(["repo_1","repo2"])
  name = can(tolist(["repo_1","repo2"])) ? ["repo_1","repo2"][floor(count.index % length(["repo_1","repo2"]))] : values(["repo_1","repo2"])[floor(count.index % length(["repo_1","repo2"]))]
}
