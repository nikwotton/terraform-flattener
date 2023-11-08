resource "github_repository" "my_repo" {
  for_each = ["repo1", "repo2"]
  name = join("", [var.prefix, each.value])
}
variable "prefix" {
  type = string
}
