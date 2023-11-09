resource "github_repository" "my_repo" {
  for_each = {
    "my_repo_1" = "my_description_1"
    "my_repo_2" = "my_description_2"
    "my_repo_3" = "my_description_3"
  }
  name = each.key
  description = each.value
}
