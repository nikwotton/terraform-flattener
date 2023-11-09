locals {
  foo = "my_"
  bar = "repo"
  foobar = join("", [local.foo, local.bar])
  repo_name = local.foobar
}

resource "github_repository" "my_repo" {
  name = local.repo_name
}
