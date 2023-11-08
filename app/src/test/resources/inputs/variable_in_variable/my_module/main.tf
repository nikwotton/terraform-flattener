module "nested_module" {
  source = "./nested_module"
  repo_name = var.repo_name
}
variable "repo_name" {
  type = string
}
