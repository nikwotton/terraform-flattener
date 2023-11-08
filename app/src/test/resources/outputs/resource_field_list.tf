resource "github_branch_protection" "my_branch_protection" {
  pattern = "master"
  push_restrictions = ["@foo","@bar"]
  repository_id = "my_repo"
}
