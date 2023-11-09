resource "github_repository" "my_repo" {
  name = join("", ["my_", "repo"])
}
