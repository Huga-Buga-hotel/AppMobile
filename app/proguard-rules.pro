# Huga Buga Hotel ProGuard / R8 Rules

# Gson / Data Models
-keep class com.example.hugabugahotel.data.model.** { *; }
-keepclassmembers class com.example.hugabugahotel.data.model.** { *; }

# Retrofit
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
-keepclassmembers class * {
    @retrofit2.http.* <methods>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Gson
-keepattributes Signature
-keepattributes *Annotation*

# Coil
-dontwarn coil.**
