resource "github_repository" "my_repo" {
  name = "my_repo"
}
resource "github_branch" "my_branch" {
  branch     = "my_branch"
  repository = github_repository.my_repo.name
}
