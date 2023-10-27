locals {
  repo_name = "my_repo"
}
resource "github_repository" "my_repo" {
  name = local.repo_name
}
