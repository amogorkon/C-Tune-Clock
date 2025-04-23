// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.chaquopy) apply false
}

// Enforce Java 17 for all modules (critical for Kotlin 2.x)
plugins.withType<JavaBasePlugin>().configureEach {
    extensions.configure<JavaPluginExtension> {
        toolchain.languageVersion.set(JavaLanguageVersion.of(17))
    }
}

// Optional: Configure Kotlin compiler for all modules
plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinBasePlugin>().configureEach {
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
        compilerOptions {
            freeCompilerArgs.addAll(
                listOf(
                    "-Xcontext-receivers",
                    "-opt-in=kotlin.RequiresOptIn"
                )
            )
        }
    }
}
