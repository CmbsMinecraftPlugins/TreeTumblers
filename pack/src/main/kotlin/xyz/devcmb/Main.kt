package xyz.devcmb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import xyz.devcmb.font.generator.ContainersGenerator
import xyz.devcmb.font.generator.DefaultAscentGenerator
import xyz.devcmb.pack.GeneratedResourcePack
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.pack.buildResourcePack
import java.io.File

fun createPackBuilder(): ResourcePackBuilder {
    return buildResourcePack {
        addFontGenerator(DefaultAscentGenerator)
        addFontGenerator(ContainersGenerator)
    }
}

fun main() {
    val pack = createPackBuilder().build()

    val outDir = System.getProperty("buildDir")
    pack.savePack(File(outDir, "pack"))
}