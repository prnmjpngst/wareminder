# Gson
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.dishub.lumajang.wareminder.data.sheets.Vehicle { *; }
-keep class com.dishub.lumajang.wareminder.data.sheets.AppsScriptApi$VehicleRaw { *; }
-keep class com.dishub.lumajang.wareminder.data.sheets.AppsScriptApi$ScriptStats { *; }
-keep class com.dishub.lumajang.wareminder.server.dto.** { *; }

# Ktor
-keep class io.ktor.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
