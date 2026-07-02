package xyz.devcmb.items.generator

import xyz.devcmb.items.GeneratedItem
import xyz.devcmb.items.ItemGenerator
import xyz.devcmb.items.ItemModelReference
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object GameIconItemGenerator : ItemGenerator {
    override fun generateItems(builder: ResourcePackBuilder): List<GeneratedItem> {
        val generatedItems: ArrayList<GeneratedItem> = ArrayList()
        val gamesFolder = javaClass.getResource("/pack/games")
        val file = File(gamesFolder!!.toURI().path)

        file.listFiles().forEach { game ->
            if(!game.isDirectory) return@forEach
            val icon = File(game, "icon.png")
            if(!icon.exists()) throw IllegalStateException("Game ${game.name} does not contain an icon.png")

            val name = "${game.name}_icon"
            val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                "game",
                *icon.path
                    .substringAfter(game.path + File.separator)
                    .split(File.separator)
                    .dropLast(1)
                    .toTypedArray(),
                name
            ))

            generatedItems.add(GeneratedItem(resource, ItemModelReference(
                IdentifiedResource(Namespace.TUMBLING, ResourcePath("item", resource.resourcePath.path))
            ).getSerialized()))

        }

        return generatedItems
    }
}