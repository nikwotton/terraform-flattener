locals {
  repos = toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ])
}

module "my_repos" {
  for_each = local.repos
  source = "./my_module"
  repo_name = each.value
}
