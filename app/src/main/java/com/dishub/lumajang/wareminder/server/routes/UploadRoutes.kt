package com.dishub.lumajang.wareminder.server.routes

import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.google.auth.oauth2.GoogleCredentials
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveText
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import android.content.Context as AndroidContext

@Singleton
class UploadRoutes @Inject constructor(
    @ApplicationContext private val appContext: AndroidContext
) {
    fun register(routing: Routing) {
        routing.post("/api/upload-service-account") {
            try {
                val fileContent = call.receiveText()

                if (fileContent.isBlank()) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "No file content"))
                    return@post
                }

                // Validate JSON format and credentials
                try {
                    GoogleCredentials.fromStream(fileContent.byteInputStream())
                        .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "Invalid service account JSON: ${e.message}"))
                    return@post
                }

                // Save to internal storage
                val file = File(appContext.filesDir, "service_account.json")
                file.writeText(fileContent)

                call.respond(ApiResponse(success = true, data = "Service account uploaded successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Any>(success = false, error = e.message))
            }
        }

        routing.post("/api/remove-service-account") {
            try {
                val file = File(appContext.filesDir, "service_account.json")
                if (file.exists()) {
                    file.delete()
                }
                call.respond(ApiResponse(success = true, data = "Service account removed"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Any>(success = false, error = e.message))
            }
        }
    }
}
