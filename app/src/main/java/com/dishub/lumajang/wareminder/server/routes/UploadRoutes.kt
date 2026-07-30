package com.dishub.lumajang.wareminder.server.routes

import android.content.Context
import com.dishub.lumajang.wareminder.server.dto.ApiResponse
import com.google.auth.oauth2.GoogleCredentials
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.request.receiveMultipart
import io.ktor.server.response.respond
import io.ktor.server.routing.Routing
import io.ktor.server.routing.post
import io.ktor.utils.io.jvm.javaio.toInputStream
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UploadRoutes @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson
) {
    fun register(routing: Routing) {
        routing.post("/api/upload-service-account") {
            try {
                val multipart = call.receiveMultipart()
                var fileContent: String? = null

                multipart.forEachPart { part ->
                    if (part.name == "file") {
                        val bytes = (part as io.ktor.http.content.PartData.FileItem).provider().readRemaining().readBytes()
                        fileContent = String(bytes, Charsets.UTF_8)
                    }
                    part.dispose()
                }

                if (fileContent == null) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "No file uploaded"))
                    return@post
                }

                // Validate JSON
                val credentials = try {
                    GoogleCredentials.fromStream(fileContent!!.byteInputStream())
                } catch (e: Exception) {
                    call.respond(HttpStatusCode.BadRequest, ApiResponse<Any>(success = false, error = "Invalid service account JSON: ${e.message}"))
                    return@post
                }

                // Save to internal storage
                val file = File(context.filesDir, "service_account.json")
                file.writeText(fileContent!!)

                call.respond(ApiResponse(success = true, data = "Service account uploaded successfully"))
            } catch (e: Exception) {
                call.respond(HttpStatusCode.InternalServerError, ApiResponse<Any>(success = false, error = e.message))
            }
        }
    }
}
