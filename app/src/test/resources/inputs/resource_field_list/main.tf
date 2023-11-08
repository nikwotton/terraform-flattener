resource "github_branch_protection" "my_branch_protection" {
  pattern       = "master"
  repository_id = "my_repo"
  push_restrictions = ["@foo", "@bar"]
}
