package xyz.devcmb.pack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.util.ConstantPackValues
import java.io.File
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds

class GeneratedResourcePack(
    val fonts: Iterable<GeneratedFont>
) {
    fun savePack(location: File) {
        println("Saving pack...")

        runBlocking(Dispatchers.IO) {
            if(location.exists()) {
                while(location.exists()) {
                    try {
                        location.deleteRecursively()
                        delay(200.milliseconds)
                    } catch (_: Exception) {}
                }
            }

            location.mkdirs()

            savePackRoot(location)
            saveFonts(location)
        }
    }

    fun savePackRoot(root: File) {
        File(root, "pack.mcmeta").writeText(
            Json.encodeToString(ConstantPackValues.PackMcMeta())
        )
    }

    fun saveFonts(root: File) {
        fonts.forEach {
            val resourcePath = ArrayList(it.resource.resourcePath.parts.toList())
            resourcePath[resourcePath.lastIndex] = "${resourcePath[resourcePath.lastIndex]}.json"

            val parent = File(Path(
                root.toString(),
                "assets",
                it.resource.namespace.name.lowercase(),
                "font"
            ).toString())
            parent.mkdirs()

            File(parent, resourcePath.last()).writeText(Json.encodeToString(it))
        }
    }
}