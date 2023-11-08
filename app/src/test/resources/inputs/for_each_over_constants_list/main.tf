resource "github_repository" "my_repo" {
  for_each   = module.constants.repo_list
  name       = each.value
}
module "constants" {
  source = "./constants"
}
