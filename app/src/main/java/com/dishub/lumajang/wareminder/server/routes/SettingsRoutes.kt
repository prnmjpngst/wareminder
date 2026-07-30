package com.dishub.lumajang.wareminder.server.routes

import com.dishub.lumajang.wareminder.data.repository.ReminderRepository
import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.route
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRoutes @Inject constructor(
    private val repository: ReminderRepository,
    private val gson: Gson
) {
    fun register(routing: Routing) {
        routing.route("/api/settings") {

            get {
                val settings = mapOf(
                    "spreadsheetId" to repository.spreadsheetId,
                    "windowStartDay" to repository.windowStartDay,
                    "windowEndDay" to repository.windowEndDay,
                    "scheduleStartHour" to repository.scheduleStartHour,
                    "scheduleEndHour" to repository.scheduleEndHour,
                    "maxPerRun" to repository.maxPerRun
                )
                call.respond(ApiResponse(success = true, data = settings))
            }

            put {
                val body = call.receiveText()
                val type = object : TypeToken<Map<String, Any>>() {}.type
                val updates: Map<String, Any> = gson.fromJson(body, type)

                updates["spreadsheetId"]?.let { repository.spreadsheetId = it.toString() }
                updates["windowStartDay"]?.let { repository.windowStartDay = (it as Double).toInt() }
                updates["windowEndDay"]?.let { repository.windowEndDay = (it as Double).toInt() }
                updates["scheduleStartHour"]?.let { repository.scheduleStartHour = (it as Double).toInt() }
                updates["scheduleEndHour"]?.let { repository.scheduleEndHour = (it as Double).toInt() }
                updates["maxPerRun"]?.let { repository.maxPerRun = (it as Double).toInt() }

                call.respond(ApiResponse(success = true, data = "Settings updated"))
            }
        }
    }
}
