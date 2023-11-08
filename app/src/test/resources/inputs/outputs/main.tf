module "my_module" {
  source = "./my_module"
}

resource "github_repository" "my_repo" {
  name = module.my_module.my_output
}
