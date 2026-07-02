package xyz.devcmb.items.generator

import xyz.devcmb.items.GeneratedItem
import xyz.devcmb.items.ItemGenerator
import xyz.devcmb.pack.ResourcePackBuilder
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Namespace
import xyz.devcmb.util.ResourcePath
import java.io.File

object PredefinedItemGenerator : ItemGenerator {
    override fun generateItems(builder: ResourcePackBuilder): List<GeneratedItem> {
        val generatedItems: ArrayList<GeneratedItem> = ArrayList()
        val models = javaClass.getResource("/pack/items")
        val file = File(models!!.toURI().path)

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

                    generatedItems.add(GeneratedItem(resource, it.readText()))
                }
            }
        }
        searchDir(file)

        return generatedItems
    }
}