package xyz.devcmb.items.generator

import xyz.devcmb.items.GeneratedItem
import xyz.devcmb.items.ItemGenerator
import xyz.devcmb.items.ItemModelReference
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object IconItemGenerator : ItemGenerator {
    override fun generateItems(builder: ResourcePackBuilder): List<GeneratedItem> {
        val games = javaClass.getResource("/pack/icons")
        val iconsFolder = File(games!!.toURI().path)
        val items = ArrayList<GeneratedItem>()

        fun searchDir(parent: File) {
            parent.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEach {
                if(it.isDirectory) searchDir(it)
                else {
                    val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath(
                        "icon",
                        *it.path
                            .substringAfter(iconsFolder.path + File.separator)
                            .substringBefore(".png")
                            .split(File.separator)
                            .toTypedArray()
                    ))

                    items.add(GeneratedItem(resource, ItemModelReference(resource).getSerialized()))
                }
            }
        }
        searchDir(iconsFolder)

        return items
    }
}