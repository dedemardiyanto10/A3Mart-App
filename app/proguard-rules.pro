# --------------------------------------------------------------------------
# 1. PERATURAN DASAR (Agar Logcat masih bisa dibaca kalau ada error)
# --------------------------------------------------------------------------
-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*
-keepattributes Signature

# --------------------------------------------------------------------------
# 2. GSON (Wajib ada agar data tidak corrupt saat dibaca)
# --------------------------------------------------------------------------
-keep class com.google.gson.** { *; }
-dontwarn sun.misc.**

# MENJAGA CLASS MODEL KAMU
# Baris ini menjaga semua class di package utama agar tidak diacak-acak.
# Jika kamu punya folder khusus model, ganti menjadi com.a3mart.app.models.**
-keep class com.a3mart.app.** { *; }

# --------------------------------------------------------------------------
# 3. MPANDROIDCHART (Agar grafik tidak hilang/error)
# --------------------------------------------------------------------------
-keep class com.github.philjay.charting.** { *; }
-dontwarn com.github.philjay.charting.**

# --------------------------------------------------------------------------
# 4. ANDROIDX & VIEW BINDING
# --------------------------------------------------------------------------
-keep class androidx.databinding.** { *; }
-keep class com.a3mart.app.databinding.** { *; }
-keep class androidx.navigation.** { *; }
-dontwarn androidx.**

# Menjaga Resource (Icon/Vector) agar tidak terhapus saat shrinking
-keep class androidx.appcompat.widget.** { *; }
-keep class com.google.android.material.** { *; }
