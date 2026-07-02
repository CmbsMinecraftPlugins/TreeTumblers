package xyz.devcmb.pack

import xyz.devcmb.font.FontGenerator
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.items.ItemGenerator
import xyz.devcmb.models.ModelGenerator
import xyz.devcmb.util.IdentifiedResource
import java.io.File

fun buildResourcePack(block: ResourcePackBuilder.() -> Unit): ResourcePackBuilder {
    val builder = ResourcePackBuilder()
    builder.block()
    return builder
}

class ResourcePackBuilder {
    val fontGenerators: HashSet<FontGenerator> = HashSet()
    val modelGenerators: HashSet<ModelGenerator> = HashSet()
    val itemGenerators: HashSet<ItemGenerator> = HashSet()
    val textures: HashSet<Pair<File, IdentifiedResource>> = HashSet()

    fun addFontGenerator(generator: FontGenerator) {
        fontGenerators.add(generator)
    }

    fun addModelGenerator(generator: ModelGenerator) {
        modelGenerators.add(generator)
    }

    fun addItemGenerator(generator: ItemGenerator) {
        itemGenerators.add(generator)
    }

    /**
     * [resource] is relative to assets/namespace/textures and should have a file extension
     */
    fun addTexture(file: File, resource: IdentifiedResource) {
        textures.add(file to resource)
    }

    fun build() : GeneratedResourcePack {
        val generatedFonts: ArrayList<GeneratedFont> = ArrayList(fontGenerators.flatMap { it.generateFonts(this) })
        val generatedModels = ArrayList(modelGenerators.flatMap { it.generateModels(this) })
        val generatedItems = ArrayList(itemGenerators.flatMap { it.generateItems(this) })

        val pack = GeneratedResourcePack(
            generatedFonts,
            generatedModels,
            generatedItems,
            textures
        )
        return pack
    }
}