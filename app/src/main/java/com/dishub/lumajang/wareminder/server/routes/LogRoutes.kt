package com.dishub.lumajang.wareminder.server.routes

import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.dishub.lumajang.wareminder.server.dto.PaginatedResponse
import io.ktor.server.application.call
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LogRoutes @Inject constructor(
    private val repository: ReminderRepository
) {
    fun register(routing: Routing) {
        routing.route("/api/logs") {

            get {
                val page = call.request.queryParameters["page"]?.toIntOrNull() ?: 1
                val pageSize = 50
                val offset = (page - 1) * pageSize
                val logs = repository.getLogs(pageSize, offset)
                val total = repository.getLogCount()

                call.respond(
                    PaginatedResponse(
                        success = true,
                        data = logs,
                        total = total,
                        page = page,
                        pageSize = pageSize
                    )
                )
            }

            get("recent") {
                val logs = repository.getLogs(20, 0)
                call.respond(ApiResponse(success = true, data = logs))
            }
        }
    }
}
