# Google Sheets API
-keep class com.google.api.services.sheets.v4.** { *; }
-keep class com.google.api.client.** { *; }
-keep class com.google.auth.** { *; }

# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.dishub.lumajang.wareminder.data.sheets.Vehicle { *; }
-keep class com.dishub.lumajang.wareminder.server.dto.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
