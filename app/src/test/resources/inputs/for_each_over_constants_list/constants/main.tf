locals {
  repo_list = ["repo_1", "repo2"]
}

output "repo_list" {
  value = local.repo_list
}
