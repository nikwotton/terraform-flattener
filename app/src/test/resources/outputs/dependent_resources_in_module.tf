resource "github_branch" "my_module_my_branch" {
  branch = "my_branch"
  repository = github_repository.my_module_my_repo.name
}
resource "github_repository" "my_module_my_repo" {
  name = "my_repo"
}
