terraform {
  required_version = "1.6.2"

  backend "s3" {
    region         = "us-east-1"
    encrypt        = true
    dynamodb_table = "my-table"
    assume_role = {
      role_arn = "arn:aws:iam::123456789:role/cicd-user"
    }
  }
}
