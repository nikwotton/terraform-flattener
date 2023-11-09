resource "github_repository" "my_repo" {
  // A dumb example, but a simplified version of one that really occurred
  for_each = [for repoName in ["repo_1", "repo_2"] : repoName if(repoName == var.repo_name)]
  name = each.value
}
variable "repo_name" {
  type = string
}
