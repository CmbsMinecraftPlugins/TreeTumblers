package xyz.devcmb.models.generator

import xyz.devcmb.models.GeneratedModel
import xyz.devcmb.models.ItemModel
import xyz.devcmb.models.ModelGenerator
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object GameIconModelGenerator : ModelGenerator {
    override fun generateModels(builder: ResourcePackBuilder): List<GeneratedModel> {
        val generatedModels: ArrayList<GeneratedModel> = ArrayList()
        val gamesFolder = javaClass.getResource("/pack/games")
        val file = File(gamesFolder!!.toURI().path)

        file.listFiles().forEach { game ->
            if(!game.isDirectory) return@forEach
            val icon = File(game, "icon.png")
            if(!icon.exists()) throw IllegalStateException("Game ${game.name} does not contain an icon.png")

            val name = "${game.name}_icon"
            val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                "item",
                "game",
                *icon.path
                    .substringAfter(game.path + File.separator)
                    .substringBefore(".png")
                    .split(File.separator)
                    .dropLast(1)
                    .toTypedArray(),
                name
            ))

            builder.addTexture(icon, IdentifiedResource(resource.path + ".png"))
            generatedModels.add(GeneratedModel(resource, ItemModel(resource).getSerialized()))
        }

        return generatedModels
    }
}