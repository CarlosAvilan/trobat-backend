val ktor_version: String by project
val kotlin_version: String by project
val logback_version: String by project

plugins {
    kotlin("jvm") version "1.9.22"
    id("io.ktor.plugin") version "2.3.12"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.9.22"
}

group = "com.trobatapp"
version = "0.0.1"

application {
    mainClass.set("io.ktor.server.netty.EngineMain")
}

repositories {
    mavenCentral()
}

dependencies {
    val ktor_version_stable = "2.3.10"
    implementation("io.ktor:ktor-server-core-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-netty-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-content-negotiation-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-serialization-kotlinx-json-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-cors-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-auth-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:$ktor_version_stable")
    implementation("io.ktor:ktor-server-auth-jwt-jvm:${ktor_version_stable}")
    implementation("org.mindrot:jbcrypt:0.4")

    implementation("ch.qos.logback:logback-classic:1.4.14")
    implementation("org.mongodb:mongodb-driver-kotlin-coroutine:5.0.0")

    testImplementation("io.ktor:ktor-server-tests-jvm:$ktor_version_stable")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit:1.9.22")
}
