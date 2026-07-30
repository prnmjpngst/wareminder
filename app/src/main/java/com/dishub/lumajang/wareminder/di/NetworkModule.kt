package com.dishub.lumajang.wareminder.di

import android.content.Context
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.auth.http.HttpCredentialsAdapter
import com.google.auth.oauth2.GoogleCredentials
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJsonFactory(): GsonFactory = GsonFactory.getDefaultInstance()

    @Provides
    @Singleton
    fun provideHttpTransport(): NetHttpTransport = GoogleNetHttpTransport.newTrustedTransport()

    @Provides
    @Singleton
    fun provideGoogleCredentials(@ApplicationContext context: Context): GoogleCredentials? {
        return try {
            // Try uploaded file first
            val uploadedFile = File(context.filesDir, "service_account.json")
            if (uploadedFile.exists()) {
                return GoogleCredentials.fromStream(uploadedFile.inputStream())
                    .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
            }

            // Fall back to assets
            val inputStream = context.assets.open("service_account.json")
            GoogleCredentials.fromStream(inputStream)
                .createScoped(listOf("https://www.googleapis.com/auth/spreadsheets"))
        } catch (e: Exception) {
            null
        }
    }

    @Provides
    @Singleton
    fun provideSheetsService(
        transport: NetHttpTransport,
        jsonFactory: GsonFactory,
        credentials: GoogleCredentials?
    ): Sheets? {
        if (credentials == null) return null
        return Sheets.Builder(transport, jsonFactory, HttpCredentialsAdapter(credentials))
            .setApplicationName("WA Reminder Dishub")
            .build()
    }
}
