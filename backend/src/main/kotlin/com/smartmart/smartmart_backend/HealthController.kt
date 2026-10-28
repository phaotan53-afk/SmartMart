
package com.smartmart.smartmart_backend

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController

@RestController
class HealthController {

    @GetMapping("/api/health")
    fun health(): Map<String, String> {
        return mapOf(
            "status" to "UP",
            "application" to "SmartMart Backend",
            "message" to "Backend is running successfully"
        )
    }
}
