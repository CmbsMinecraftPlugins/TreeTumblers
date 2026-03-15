import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

plugins {
    kotlin("jvm") version "2.3.20-RC"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "xyz.devcmb"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.panda-lang.org/releases")
    maven { url = uri("https://jitpack.io") }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.11.2")
    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("dev.rollczi:litecommands-bukkit:3.10.9")
    implementation("com.github.29cmb.InvControl:invcontrol-core:v0.2.2")
    implementation("commons-io:commons-io:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        dependsOn("build")
        minecraftVersion("1.21.11")
    }
}

val targetJavaVersion = 21
kotlin {
    jvmToolchain(targetJavaVersion)
}

tasks.compileJava {
    options.compilerArgs.add("-parameters")
}

tasks.build {
    dependsOn("updateVersion", "shadowJar")
}

fun padLeftZeros(inputString: String, length: Int): String {
    if (inputString.length >= length) return inputString
    val sb = StringBuilder()
    while (sb.length < length - inputString.length) {
        sb.append('0')
    }
    sb.append(inputString)
    return sb.toString()
}

tasks.register("updateVersion") {
    doLast {
        val versionFile = file("src/main/kotlin/xyz/devcmb/tumblers/Constants.kt")
        val versionCounterFile = file("version.txt")
        val newVersion = project.version.toString()

        var counter = 0
        if (versionCounterFile.exists()) {
            counter = versionCounterFile.readText().trim().toInt(16)
        }

        counter++
        val hexCounter = counter.toString(16)
        val updatedVersion = "$newVersion-${padLeftZeros(hexCounter, 6)}"
        val content = versionFile.readText()
        val updatedContent = content.replace(
            Regex("""(const val VERSION: String = ")([^"]+)(")"""),
            "$1$updatedVersion$3"
        )

        Files.write(
            Paths.get(versionFile.toURI()),
            updatedContent.toByteArray(),
            StandardOpenOption.TRUNCATE_EXISTING
        )
        versionCounterFile.writeText(hexCounter)
    }
}

tasks.processResources {
    val props = mapOf("version" to version)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") {
        expand(props)
    }
}
