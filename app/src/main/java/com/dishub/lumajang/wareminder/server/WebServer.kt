package com.dishub.lumajang.wareminder.server

import android.content.Context
import android.util.Log
import com.dishub.lumajang.wareminder.server.routes.LogRoutes
import com.dishub.lumajang.wareminder.server.routes.SendRoutes
import com.dishub.lumajang.wareminder.server.routes.SettingsRoutes
import com.dishub.lumajang.wareminder.server.routes.StaticRoutes
import com.dishub.lumajang.wareminder.server.routes.UploadRoutes
import com.dishub.lumajang.wareminder.server.routes.VehicleRoutes
import dagger.hilt.android.qualifiers.ApplicationContext
import io.ktor.server.engine.ApplicationEngine
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.routing.routing
import io.ktor.server.application.install
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.gson.gson
import io.ktor.server.plugins.cors.CORS
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebServer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val vehicleRoutes: VehicleRoutes,
    private val sendRoutes: SendRoutes,
    private val logRoutes: LogRoutes,
    private val settingsRoutes: SettingsRoutes,
    private val uploadRoutes: UploadRoutes,
    private val staticRoutes: StaticRoutes
) {
    companion object {
        private const val TAG = "WebServer"
        private const val PORT = 8080
    }

    private var server: ApplicationEngine? = null
    @Volatile private var running = false

    fun start() {
        if (running) return
        try {
            server = embeddedServer(Netty, port = PORT, host = "0.0.0.0") {
                install(ContentNegotiation) {
                    gson {
                        setPrettyPrinting()
                    }
                }

                install(CORS) {
                    anyHost()
                    allowHeader(HttpHeaders.ContentType)
                    allowHeader(HttpHeaders.Authorization)
                    allowMethod(HttpMethod.Get)
                    allowMethod(HttpMethod.Post)
                    allowMethod(HttpMethod.Put)
                    allowMethod(HttpMethod.Delete)
                }

                routing {
                    vehicleRoutes.register(this)
                    sendRoutes.register(this)
                    logRoutes.register(this)
                    settingsRoutes.register(this)
                    uploadRoutes.register(this)
                    staticRoutes.register(this)
                }
            }.start(wait = false)
            running = true
            Log.d(TAG, "Web server started on port $PORT")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start web server", e)
        }
    }

    fun stop() {
        server?.stop(1000, 2000)
        server = null
        running = false
        Log.d(TAG, "Web server stopped")
    }

    fun isRunning(): Boolean = running

    fun getPort(): Int = PORT
}
