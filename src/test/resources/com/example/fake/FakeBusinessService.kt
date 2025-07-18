package com.example.fake

import java.util.*

/**
 * Fake business service class for testing purposes
 */
class FakeBusinessService(private val serviceName: String) {

    private val cache: MutableMap<String, Any> = mutableMapOf()

    fun processRequest(requestId: String, data: Map<String, Any>): ProcessResult {
        validateRequest(requestId, data)

        val result = performBusinessLogic(data)
        cache[requestId] = result

        return ProcessResult(
            requestId = requestId,
            success = true,
            data = result,
            timestamp = Date()
        )
    }

    fun getServiceStatus(): ServiceStatus {
        return ServiceStatus(
            name = serviceName,
            isHealthy = true,
            cacheSize = cache.size,
            uptime = System.currentTimeMillis()
        )
    }

    private fun validateRequest(requestId: String, data: Map<String, Any>) {
        require(requestId.isNotEmpty()) { "Request ID cannot be empty" }
        require(data.isNotEmpty()) { "Data cannot be empty" }
    }

    private fun performBusinessLogic(data: Map<String, Any>): Map<String, Any> {
        // Simulate some business logic
        val processed = data.mapValues { (key, value) ->
            when (value) {
                is String -> value.uppercase()
                is Number -> value.toDouble() * 1.1
                else -> value
            }
        }

        return processed + ("processed_at" to System.currentTimeMillis())
    }
}

data class ProcessResult(
    val requestId: String,
    val success: Boolean,
    val data: Map<String, Any>,
    val timestamp: Date
)

data class ServiceStatus(
    val name: String,
    val isHealthy: Boolean,
    val cacheSize: Int,
    val uptime: Long
)
