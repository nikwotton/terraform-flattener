terraform {
  backend "s3" {
    assume_role = {
      role_arn = "arn:aws:iam::123456789:role/cicd-user"
    }
    dynamodb_table = "my-table"
    encrypt = true
    region = "us-east-1"
  }
  required_version = "1.6.2"
}
