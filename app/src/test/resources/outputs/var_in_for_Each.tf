resource "github_repository" "my_module_my_repo" {
  count = length([for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")])
  name = can(tolist([for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")])) ? [for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")][floor(count.index % length([for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")]))] : values([for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")])[floor(count.index % length([for repoName in ["repo_1", "repo_2"] : repoName if(repoName == "repo_1")]))]
}
