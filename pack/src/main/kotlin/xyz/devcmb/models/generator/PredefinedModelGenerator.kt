package xyz.devcmb.models.generator

import xyz.devcmb.models.GeneratedModel
import xyz.devcmb.models.ModelGenerator
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object PredefinedModelGenerator : ModelGenerator {
    override fun generateModels(builder: ResourcePackBuilder): List<GeneratedModel> {
        val generatedModels: ArrayList<GeneratedModel> = ArrayList()
        val models = javaClass.getResource("/pack/models")
        val file = File(models!!.toURI().path)

        val modelItemTextures = javaClass.getResource("/pack/model_item_textures")
        val modelItemTexturesFile = File(modelItemTextures!!.toURI().path)

        val modelBlockTextures = javaClass.getResource("/pack/model_block_textures")
        val modelBlockTexturesFile = File(modelBlockTextures!!.toURI().path)

        fun searchDir(parent: File) {
            parent.listFiles().filter { it.name.endsWith(".json") || it.isDirectory }.forEach {
                if(it.isDirectory) searchDir(it)
                else {
                    val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                        *it.path
                            .substringAfter(file.path + File.separator)
                            .substringBefore(".json")
                            .split(File.separator)
                            .toTypedArray()
                    ))

                    generatedModels.add(GeneratedModel(resource, it.readText()))
                }
            }
        }

        fun searchModelTextures(root: File, parent: File, type: String) {
            parent.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEach {
                if(it.isDirectory) searchModelTextures(root, it, type)
                else {
                    val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                        type,
                        *it.path
                            .substringAfter(root.path + File.separator)
                            .split(File.separator)
                            .toTypedArray()
                    ))

                    builder.addTexture(it, resource)
                }
            }
        }

        searchDir(file)
        searchModelTextures(modelItemTexturesFile, modelItemTexturesFile, "item")
        searchModelTextures(modelBlockTexturesFile, modelBlockTexturesFile, "block")

        return generatedModels
    }
}