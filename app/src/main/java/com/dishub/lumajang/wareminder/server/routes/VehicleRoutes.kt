package com.dishub.lumajang.wareminder.server.routes

import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import com.dishub.lumajang.wareminder.data.sheets.Vehicle
import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.dishub.lumajang.wareminder.server.dto.PaginatedResponse
import com.dishub.lumajang.wareminder.server.dto.StatsResponse
import com.google.gson.Gson
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.put
import io.ktor.server.routing.delete
import io.ktor.server.routing.route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VehicleRoutes @Inject constructor(
    private val repository: ReminderRepository,
    private val gson: Gson
) {
    fun register(routing: Routing) {
        routing.route("/api/vehicles") {
            get {
                val vehicles = repository.getCachedVehicles()
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val search = call.request.queryParameters["search"]?.lowercase() ?: ""
                val pageSize = 50

                val filtered = if (search.isNotBlank()) {
                    vehicles.filter {
                        it.nomorKendaraan.lowercase().contains(search) ||
                        it.namaPemilik.lowercase().contains(search) ||
                        it.nomorHP.contains(search)
                    }
                } else vehicles

                val total = filtered.size
                val paged = filtered.drop((page - 1) * pageSize).take(pageSize)

                call.respond(
                    PaginatedResponse(
                        success = true,
                        data = paged,
                        total = total,
                        page = page,
                        pageSize = pageSize
                    )
                )
            }

            get("{row}") {
                val row = call.parameters["row"]?.toIntOrNull() ?: return@get call.respond(
                    HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "Invalid row")
                )
                val vehicle = repository.getCachedVehicles().find { it.rowIndex == row }
                if (vehicle == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Any>(success = false, error = "Not found"))
                } else {
                    call.respond(ApiResponse(success = true, data = vehicle))
                }
            }
        }

        routing.get("/api/stats") {
            val stats = repository.getStats()
            call.respond(
                StatsResponse(
                    totalVehicles = stats["totalVehicles"] as? Int ?: 0,
                    expiredVehicles = stats["expiredVehicles"] as? Int ?: 0,
                    expiringSoon = stats["expiringSoon"] as? Int ?: 0,
                    activeVehicles = stats["activeVehicles"] as? Int ?: 0,
                    sentToday = stats["sentToday"] as? Int ?: 0,
                    serviceRunning = stats["serviceRunning"] as? Boolean ?: false,
                    lastSync = stats["lastSync"] as? Long ?: 0,
                    lastCheck = stats["lastCheck"] as? Long ?: 0,
                    spreadsheetConfigured = stats["spreadsheetConfigured"] as? Boolean ?: false,
                    sheetsAvailable = stats["sheetsAvailable"] as? Boolean ?: false
                )
            )
        }

        routing.get("/api/status") {
            call.respond(
                com.dishub.lumajang.wareminder.server.dto.StatusResponse(
                    serviceRunning = repository.isServiceRunning(),
                    accessibilityEnabled = com.dishub.lumajang.wareminder.service.WaAutoSendService.hasInstance(),
                    lastCheck = repository.lastCheckTime,
                    lastSync = repository.lastSyncTime,
                    uptime = System.currentTimeMillis()
                )
            )
        }
    }
}
