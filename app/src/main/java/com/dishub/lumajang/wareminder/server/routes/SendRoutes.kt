package com.dishub.lumajang.wareminder.server.routes

import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.dishub.lumajang.wareminder.service.WaAutoSendService
import com.dishub.lumajang.wareminder.service.WaIntentSender
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.server.routing.route
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SendRoutes @Inject constructor(
    private val repository: ReminderRepository,
    private val waIntentSender: WaIntentSender
) {
    fun register(routing: Routing) {
        routing.route("/api/send") {

            post("all") {
                withContext(Dispatchers.IO) {
                    repository.syncFromSheets()
                    val eligible = repository.getEligibleVehicles()
                    val batch = eligible.take(repository.maxPerRun)
                    var sent = 0
                    var failed = 0

                    for ((index, vehicle) in batch.withIndex()) {
                        try {
                            waIntentSender.send(vehicle)
                            Thread.sleep(1500)

                            if (WaAutoSendService.hasInstance()) {
                                WaAutoSendService.sendWithAutoClick {}
                            }

                            repository.markSent(vehicle)
                            sent++

                            if (index < batch.size - 1) {
                                Thread.sleep((10_000L..40_000L).random())
                            }
                        } catch (e: Exception) {
                            repository.markFailed(vehicle, e.message ?: "Error")
                            failed++
                        }
                    }

                    call.respond(
                        ApiResponse(
                            success = true,
                            data = mapOf("sent" to sent, "failed" to failed, "total" to batch.size)
                        )
                    )
                }
            }

            post("{row}") {
                val row = call.parameters["row"]?.toIntOrNull()
                if (row == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "Invalid row"))
                    return@post
                }

                val vehicle = repository.getCachedVehicles().find { it.rowIndex == row }
                if (vehicle == null) {
                    call.respond(HttpStatusCode.NotFound, ApiResponse<Any>(success = false, error = "Vehicle not found"))
                    return@post
                }

                try {
                    waIntentSender.send(vehicle)
                    repository.markSent(vehicle)
                    call.respond(ApiResponse(success = true, data = vehicle))
                } catch (e: Exception) {
                    repository.markFailed(vehicle, e.message ?: "Error")
                    call.respond(HttpStatusCode.InternalServerError, ApiResponse<Any>(success = false, error = e.message))
                }
            }

            post("sync") {
                withContext(Dispatchers.IO) {
                    val result = repository.syncFromSheets()
                    if (result.isSuccess) {
                        call.respond(ApiResponse(success = true, data = mapOf("count" to result.getOrNull()?.size)))
                    } else {
                        call.respond(
                            HttpStatusCode.InternalServerError,
                            ApiResponse<Any>(success = false, error = result.exceptionOrNull()?.message)
                        )
                    }
                }
            }
        }
    }
}
