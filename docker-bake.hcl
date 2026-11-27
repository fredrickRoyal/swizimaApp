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
    "stafftrackapi",
    "filesapi",
    "whatsappapi",
    "apigateway",
    "stafftrack-web",
    "swizima-web"
  ]
}

group "web" {
  targets = [
    "swizima-web",
    "stafftrack-web"
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

target "stafftrackapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "stafftrackapi"
  tags       = ["${DOCKERHUB_USER}/swizima-stafftrackapi:${TAG}"]
}

target "filesapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "filesapi"
  tags       = ["${DOCKERHUB_USER}/swizima-filesapi:${TAG}"]
}

target "apigateway" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "apigateway"
  tags       = ["${DOCKERHUB_USER}/swizima-apigateway:${TAG}"]
}

target "whatsappapi" {
  context    = "."
  dockerfile = "Dockerfile"
  target     = "whatsappapi"
  tags       = ["${DOCKERHUB_USER}/swizima-whatsappapi:${TAG}"]
}

target "swizima-web" {
  context    = "swizima-web"
  dockerfile = "Dockerfile"
  tags       = ["${DOCKERHUB_USER}/swizima-web:${TAG}"]
}

target "stafftrack-web" {
  context    = "stafftrack-web"
  dockerfile = "Dockerfile"
  tags       = ["${DOCKERHUB_USER}/stafftrack-web:${TAG}"]
}

