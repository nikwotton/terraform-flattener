resource "github_repository" "my_repos_my_repo" {
  count = length(toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ]))
  name = can(tolist(toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ]))) ? toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ])[floor(count.index % length(toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ])))] : values(toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ]))[floor(count.index % length(toset([
    "my_repo_1",
    "my_repo_2",
    "my_repo_3",
    "my_repo_4",
    "my_repo_5",
  ])))]
}
