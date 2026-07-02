package xyz.devcmb.pack

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.GeneratedFont

fun buildResourcePack(block: ResourcePackBuilder.() -> Unit): ResourcePackBuilder {
    val builder = ResourcePackBuilder()
    builder.block()
    return builder
}

class ResourcePackBuilder {
    val generators: HashSet<FontGenerator> = HashSet()

    fun addFontGenerator(generator: FontGenerator) {
        generators.add(generator)
    }

    fun build() : GeneratedResourcePack {
        val generatedFonts: ArrayList<GeneratedFont> = ArrayList(generators.flatMap { it.generateFonts() })
        val pack = GeneratedResourcePack(generatedFonts)
        return pack
    }
}