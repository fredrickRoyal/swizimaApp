package com.example.swizima.data

object ApiGatewayConfig {
    // API Gateway Base URL routing requests to downstream microservices (Auth, Users, Services, etc.)
    var gatewayBaseUrl: String = "https://api.swizima.com/gateway/v1"

    // When true, uses robust mock/simulation responses if the API gateway is unreachable
    var isSimulationMode: Boolean = true
}
