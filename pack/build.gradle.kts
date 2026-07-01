plugins {
    kotlin("jvm") version "2.3.20-RC"
    application
}

repositories {
    mavenCentral()
}

dependencies {
}

kotlin {
    jvmToolchain(25)
}

application {
    mainClass.set("xyz.devcmb.MainKt")
}