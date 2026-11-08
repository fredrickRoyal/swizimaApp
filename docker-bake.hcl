variable "DOCKERHUB_USER" {
  default = "fredrickkasoma"
}

variable "TAG" {
  default = "latest"
}

group "default" {
  targets = [
    "usersapi",
    "clientapi",
    "configurationsapi",
    "paymentsapi",
    "clienttransactionsapi",
    "apigateway"
  ]
}

target "usersapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "usersapi"
  tags       = ["${DOCKERHUB_USER}/swizima-usersapi:${TAG}"]
}

target "clientapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "clientapi"
  tags       = ["${DOCKERHUB_USER}/swizima-clientapi:${TAG}"]
}

target "configurationsapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "configurationsapi"
  tags       = ["${DOCKERHUB_USER}/swizima-configurationsapi:${TAG}"]
}

target "paymentsapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "paymentsapi"
  tags       = ["${DOCKERHUB_USER}/swizima-paymentsapi:${TAG}"]
}

target "clienttransactionsapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "clienttransactionsapi"
  tags       = ["${DOCKERHUB_USER}/swizima-clienttransactionsapi:${TAG}"]
}

target "apigateway" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "apigateway"
  tags       = ["${DOCKERHUB_USER}/swizima-apigateway:${TAG}"]
}

