package xyz.devcmb

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import xyz.devcmb.font.generator.DefaultAscentGenerator
import xyz.devcmb.pack.buildResourcePack
import java.io.File

val coroutineScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

fun main() {
    val pack = buildResourcePack {
        addFontGenerator(DefaultAscentGenerator)
    }.build()

    val outDir = System.getProperty("buildDir")
    pack.savePack(File(outDir, "pack"))
}