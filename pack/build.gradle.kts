plugins {
    kotlin("jvm") version "2.3.20-RC"
    application
    kotlin("plugin.serialization") version "2.4.0"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
}

dependencies {
    implementation(libs.paper.api)
    implementation(libs.kotlinx.serialization)
    implementation(libs.kotlinx.coroutines)
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("xyz.devcmb.MainKt")
}

tasks.named<JavaExec>("run") {
    systemProperty(
        "buildDir",
        layout.buildDirectory.get().asFile.absolutePath
    )
}