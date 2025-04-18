# Keep Chaquopy/Python classes
-keep class com.chaquo.python.** { *; }
-keep class org.kamranzafar.jtar.** { *; }

# Keep Python modules and native libraries
-keep class com.chaquo.python.internal.** { *; }
-keep class org.kamranzafar.jtar.** { *; }

# Keep Kotlin metadata (critical for Compose + Python interop)
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepclassmembers class **.R* { public static <fields>; }

# Preserve line numbers for debugging
-keepattributes SourceFile,LineNumberTable

# Compose-specific rules (if using reflection)
-keep class androidx.compose.runtime.Composer { *; }
-keep class kotlinx.coroutines.** { *; }

# Uncomment to hide source file names in release
# -renamesourcefileattribute SourceFile