package xyz.devcmb

import xyz.devcmb.font.generator.*
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.pack.buildResourcePack
import java.io.File

fun createPackBuilder(): ResourcePackBuilder {
    return buildResourcePack {
        addFontGenerator(DefaultAscentGenerator)
        addFontGenerator(ContainersGenerator)
        addFontGenerator(GameAssetGenerator)
        addFontGenerator(IconsGenerator)
        addFontGenerator(SpacesGenerator)
        addFontGenerator(HudAssetGenerator)
    }
}

fun main() {
    val pack = createPackBuilder().build()

    val outDir = System.getProperty("buildDir")
    pack.savePack(File(outDir, "pack"))
}