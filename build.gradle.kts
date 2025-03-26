plugins { id("buildsrc.convention.kotlin-jvm") }

subprojects {

    // For projects using the Kotlin plugin:
    plugins.withType<org.jetbrains.kotlin.gradle.plugin.KotlinPluginWrapper> {
        // This ensures Kotlin compiles with Java 21 toolchain
        kotlin {
            jvmToolchain(21)
        }
    }
}

