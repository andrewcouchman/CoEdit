plugins {
    id("buildsrc.convention.kotlin-jvm")

    application
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(platform("org.http4k:http4k-bom:6.2.0.0"))

    implementation("org.http4k:http4k-core")
    implementation("org.http4k:http4k-server-undertow")
    implementation("org.http4k:http4k-client-websocket")
    implementation("org.http4k:http4k-format-jackson")
}

application {
    mainClass = "com.andrewcouchman.coedit.BackendKt"
}
