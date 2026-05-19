# ============================================================
#  HOMNEY — ProGuard / R8 rules para release
# ============================================================

# Conservar información de línea en stack traces (útil para Crashlytics)
-keepattributes SourceFile,LineNumberTable
-renamesourcefileattribute SourceFile

# ── GSON ─────────────────────────────────────────────────────
# Los modelos de datos se serializan/deserializan por reflexión;
# sin estas reglas R8 elimina sus campos y getters en release.
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**

-keep class com.google.gson.** { *; }
-keep class com.google.gson.stream.** { *; }

# Conservar todos los modelos del paquete webservice (DTOs que parsea Gson)
-keep class com.homney.app.webservice.modelo.** { *; }
-keep class com.homney.app.webservice.respuestas.** { *; }

# ── VOLLEY ───────────────────────────────────────────────────
-keep class com.android.volley.** { *; }
-keep interface com.android.volley.** { *; }

# ── GLIDE ────────────────────────────────────────────────────
-keep public class * implements com.bumptech.glide.module.GlideModule
-keep class * extends com.bumptech.glide.module.AppGlideModule {
    <init>(...);
}
-keep public enum com.bumptech.glide.load.ImageHeaderParser$** {
    **[] $VALUES;
    public *;
}

# ── ANYCHART ─────────────────────────────────────────────────
-keep class com.anychart.** { *; }
-dontwarn com.anychart.**

# ── FIREBASE / GOOGLE SIGN-IN ────────────────────────────────
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }
-dontwarn com.google.firebase.**
-dontwarn com.google.android.gms.**

# ── ANDROIDX / NAVIGATION ────────────────────────────────────
-keep class androidx.navigation.** { *; }

# ── WEBVIEW CON JAVASCRIPT (no usado actualmente) ────────────
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}