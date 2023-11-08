resource "github_repository" "my_repo" {
  name = var.repo_name
}
variable "repo_name" {
  type = string
}
