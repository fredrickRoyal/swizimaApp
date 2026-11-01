package com.example.swizima.data

object ApiGatewayConfig {
    // API Gateway Base URL:
    // - Use "http://10.0.2.2:9080/api/v1" if running on the Android Emulator (10.0.2.2 maps to host PC localhost)
    // - Use "http://192.168.100.6:9080/api/v1" if running on a Physical Android Device (ensure Windows Firewall allows port 9080)
    var gatewayBaseUrl: String = "http://192.168.100.6:9080/api/v1"

    // When true, uses mock/simulation responses if offline
    var isSimulationMode: Boolean = false
}
