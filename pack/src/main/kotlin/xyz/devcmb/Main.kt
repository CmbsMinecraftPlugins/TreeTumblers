package xyz.devcmb

import xyz.devcmb.font.generator.*
import xyz.devcmb.items.generator.*
import xyz.devcmb.models.generator.*
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.pack.buildResourcePack
import xyz.devcmb.util.Logger
import java.io.File
import kotlin.io.path.Path

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

    val outDirs: HashMap<File, Boolean> = hashMapOf(File(System.getProperty("buildDir")) to false)
    val envOutput = System.getenv("RESOURCE_PACK_BUILD_LOCATION")
    if(envOutput != null) {
        val outputDirs = envOutput.split(';')
        outDirs.putAll(outputDirs.map { File(it) to false })
    }

    val serverDir = System.getProperty("serverDir")
    val packOutputDir = File(Path(serverDir, "plugins", "TreeTumblers").toString())
    if(packOutputDir.exists()) {
        outDirs[packOutputDir] = true
    } else {
        Logger.warn("Server directory does not exist or does not contain a Tree Tumblers data directory, skipping")
    }

    pack.savePack(outDirs)
}