package xyz.devcmb.pack

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.util.IdentifiedResource
import java.io.File

fun buildResourcePack(block: ResourcePackBuilder.() -> Unit): ResourcePackBuilder {
    val builder = ResourcePackBuilder()
    builder.block()
    return builder
}

class ResourcePackBuilder {
    val generators: HashSet<FontGenerator> = HashSet()
    val textures: HashSet<Pair<File, IdentifiedResource>> = HashSet()

    fun addFontGenerator(generator: FontGenerator) {
        generators.add(generator)
    }

    fun addTexture(file: File, parent: IdentifiedResource) {
        textures.add(file to parent)
    }

    fun build() : GeneratedResourcePack {
        val generatedFonts: ArrayList<GeneratedFont> = ArrayList(generators.flatMap { it.generateFonts(this) })
        val pack = GeneratedResourcePack(generatedFonts, textures)
        return pack
    }
}