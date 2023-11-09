resource "github_repository" "my_repo" {
  for_each = toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ])
  name = each.key
}
