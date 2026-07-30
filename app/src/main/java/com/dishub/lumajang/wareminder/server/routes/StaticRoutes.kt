package com.dishub.lumajang.wareminder.server.routes

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.http.ContentType
import io.ktor.server.application.call
import io.ktor.server.response.respondText
import io.ktor.server.routing.Routing
import io.ktor.server.routing.get
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StaticRoutes @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun register(routing: Routing) {
        routing.get("/") {
            serveAsset("index.html")?.let { html ->
                call.respondText(html, ContentType.Text.Html)
            } ?: call.respondText("Server running", ContentType.Text.Plain)
        }

        routing.get("{path...}") {
            val path = call.parameters["path"] ?: return@get
            if (path.startsWith("api/")) return@get
            val mime = when {
                path.endsWith(".js") -> ContentType.Application.JavaScript
                path.endsWith(".css") -> ContentType.Text.CSS
                path.endsWith(".html") -> ContentType.Text.Html
                path.endsWith(".svg") -> ContentType.Image.SVG
                path.endsWith(".png") -> ContentType.Image.PNG
                path.endsWith(".json") -> ContentType.Application.Json
                path.endsWith(".woff2") -> ContentType("font", "woff2")
                else -> ContentType.Text.Plain
            }
            serveAsset(path)?.let { data ->
                call.respondText(data, mime)
            }
        }
    }

    private fun serveAsset(path: String): String? {
        return try {
            context.assets.open("web/$path").bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            null
        }
    }
}
