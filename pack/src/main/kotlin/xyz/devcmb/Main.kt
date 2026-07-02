package xyz.devcmb

import xyz.devcmb.font.generator.*
import xyz.devcmb.items.generator.*
import xyz.devcmb.models.generator.*
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

        addModelGenerator(PredefinedModelGenerator)
        addModelGenerator(IconItemModelGenerator)
        addModelGenerator(GameIconModelGenerator)

        addItemGenerator(PredefinedItemGenerator)
        addItemGenerator(IconItemGenerator)
        addItemGenerator(GameIconItemGenerator)
    }
}

fun main() {
    val pack = createPackBuilder().build()

    val outDir = System.getenv("RESOURCE_PACK_BUILD_LOCATION") ?: System.getProperty("buildDir")
    pack.savePack(File(outDir, "pack"))
    println("Output pack to $outDir")
}