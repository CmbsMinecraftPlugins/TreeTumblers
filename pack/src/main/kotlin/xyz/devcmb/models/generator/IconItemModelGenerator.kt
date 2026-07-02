package xyz.devcmb.models.generator

import xyz.devcmb.models.GeneratedModel
import xyz.devcmb.models.ItemModel
import xyz.devcmb.models.ModelGenerator
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object IconItemModelGenerator : ModelGenerator {
    override fun generateModels(builder: ResourcePackBuilder): List<GeneratedModel> {
        val games = javaClass.getResource("/pack/icons")
        val iconsFolder = File(games!!.toURI().path)
        val models = ArrayList<GeneratedModel>()

        fun searchDir(parent: File) {
            parent.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEach {
                if(it.isDirectory) searchDir(it)
                else {
                    val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                        "item",
                        "icon",
                        *it.path
                            .substringAfter(iconsFolder.path + File.separator)
                            .substringBefore(".png")
                            .split(File.separator)
                            .toTypedArray()
                    ))

                    // Resource is the same but its in a different content root
                    builder.addTexture(it, IdentifiedResource(resource.path + ".png"))
                    models.add(GeneratedModel(resource, ItemModel(resource).getSerialized()))
                }
            }
        }
        searchDir(iconsFolder)

        return models
    }
}