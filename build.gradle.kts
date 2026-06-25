import org.gradle.kotlin.dsl.support.serviceOf
import java.io.ByteArrayOutputStream
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

plugins {
    kotlin("jvm") version "2.3.20-RC"
    id("com.gradleup.shadow") version "8.3.0"
    id("xyz.jpenilla.run-paper") version "3.0.2"
    kotlin("plugin.serialization").version("2.2.20")
}

group = "xyz.devcmb"
version = "1.0.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") {
        name = "papermc-repo"
    }
    maven("https://repo.panda-lang.org/releases")
    maven("https://jitpack.io")
    maven("https://maven.enginehub.org/repo/")
    maven("https://mvn.lib.co.nz/public")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://maven.noxcrew.com/public")
    maven("https://repo.viaversion.com")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:26.1.2.build.+")
    compileOnly("com.fastasyncworldedit:FastAsyncWorldEdit-Bukkit:2.15.2")
    compileOnly("com.github.retrooper:packetevents-spigot:2.12.1")
    compileOnly("me.libraryaddict.disguises:libsdisguises:11.0.16")

    // https://discord.com/channels/707193125478596668/1134515300742733985/1502698083354673186
    // lucydotp (roughly): it has changed from `paper` to `paper-platform`
    // thank you lucy
    compileOnly("com.noxcrew.noxesium:paper-platform:3.1.0")

    implementation("org.reflections:reflections:0.10.2")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("dev.rollczi:litecommands-bukkit:3.10.9")
    implementation("dev.rollczi:litecommands-adventure:3.10.2")
    implementation("com.github.29cmb.InvControl:invcontrol-core:v0.2.6")
    implementation("commons-io:commons-io:2.14.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation("io.ktor:ktor-client-core:3.4.3")
    implementation("io.ktor:ktor-client-cio:3.4.3")
    implementation("io.ktor:ktor-client-content-negotiation:3.4.3")
    implementation("io.ktor:ktor-serialization-kotlinx-json:3.4.3")
    implementation(kotlin("reflect"))
}

tasks {
    runServer {
        // Configure the Minecraft version for our task.
        // This is the only required configuration besides applying the plugin.
        // Your plugin's jar (or shadowJar if present) will be used automatically.
        dependsOn("build")
        downloadPlugins {
            modrinth("packetevents", "2.12.1+spigot")
            modrinth("axiom-paper-plugin", "5.0.4+26.1")
            modrinth("noxesium-paper-api", "3.1.0")
            // The modrinth release shares the version tag for paper and spigot, and the spigot version crashes
            github("IntellectualSites", "FastAsyncWorldEdit", "2.15.2", "FastAsyncWorldEdit-Paper-2.15.2.jar")
            github("libraryaddict", "LibsDisguises", "v11.0.18", "LibsDisguises-11.0.18-Github.jar")
        }
        minecraftVersion("26.1.2")
    }
}

val targetJavaVersion = 25
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
    description = "Update the version file"
    val execOps = project.serviceOf<ExecOperations>()
    doLast {
        val versionFile = file("src/main/kotlin/xyz/devcmb/tumblers/Constants.kt")
        val versionCounterFile = file("version.txt")

        var counter = 0
        if (versionCounterFile.exists()) {
            counter = versionCounterFile.readText().trim().toInt(16)
        }

        counter++
        val hexCounter = counter.toString(16)
        val updatedVersion = padLeftZeros(hexCounter, 3)
        val content = versionFile.readText()

        fun gitBranch(): String {
            return try {
                val byteOut = ByteArrayOutputStream()
                execOps.exec {
                    commandLine = "git rev-parse --abbrev-ref HEAD".split(" ")
                    standardOutput = byteOut
                }
                String(byteOut.toByteArray()).trim().let {
                    if (it == "HEAD") "detached" else it
                }
            } catch (e: Exception) {
                logger.warn("Unable to determine current branch: ${e.message}")
                "unknown"
            }
        }

        val updatedContent = content.replace(
            Regex("""(const val VERSION: String = ")([^"]+)(")"""),
            "$1$updatedVersion$3"
        ).replace(
            Regex("""(const val BRANCH: String = ")([^"]+)(")"""),
            "$1${gitBranch()}$3"
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
