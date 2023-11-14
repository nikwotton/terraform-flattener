resource "github_repository" "my_repo" {
  name = "my_repo"
  dynamic "pages" {
    for_each = [
      {
        source_branch = "gh-pages"
        source_path   = "/"
      }
    ]
    content {
      source {
        branch = pages.value.source_branch
        path   = pages.value.source_path
      }
    }
  }
}
