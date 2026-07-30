package com.dishub.lumajang.wareminder.server.dto

data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null
)

data class PaginatedResponse<T>(
    val success: Boolean,
    val data: List<T>,
    val total: Int,
    val page: Int,
    val pageSize: Int
)

data class StatsResponse(
    val totalVehicles: Int,
    val expiredVehicles: Int,
    val expiringSoon: Int,
    val activeVehicles: Int,
    val sentToday: Int = 0,
    val serviceRunning: Boolean,
    val lastSync: Long,
    val lastCheck: Long,
    val spreadsheetConfigured: Boolean,
    val sheetsAvailable: Boolean
)

data class StatusResponse(
    val serviceRunning: Boolean,
    val accessibilityEnabled: Boolean,
    val lastCheck: Long,
    val lastSync: Long,
    val uptime: Long,
    val appVersion: String = "1.0.0"
)
