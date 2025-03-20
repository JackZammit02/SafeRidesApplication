plugins {
    alias(libs.plugins.jetbrains.kotlin.jvm)
    kotlin("plugin.serialization") version "2.1.10"
    id("io.ktor.plugin") version "3.1.1"
}

dependencies {
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation.jvm)
    implementation(libs.ktor.serialization.kotlinx.json.jvm)
    implementation(libs.kotlinx.serialization.json)
    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation(libs.firebase.firestore.ktx)
}


java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.example.backend.ApplicationKt")
}

