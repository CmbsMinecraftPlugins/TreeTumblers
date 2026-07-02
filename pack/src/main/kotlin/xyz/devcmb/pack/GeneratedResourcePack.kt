package xyz.devcmb.pack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.util.ConstantPackValues
import xyz.devcmb.util.IdentifiedResource
import java.io.File
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.milliseconds

class GeneratedResourcePack(
    val fonts: Iterable<GeneratedFont>,
    val textures: HashSet<Pair<File, IdentifiedResource>>
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
            saveOverrides(location)
            saveTextures(location)
            saveFonts(location)
        }
    }

    private fun savePackRoot(root: File) {
        val pack = object {}.javaClass.getResource("/pack/pack.png")
        File(root, "pack.png").outputStream().use {
            pack!!.openStream().copyTo(it)
        }

        File(root, "pack.mcmeta").writeText(
            Json.encodeToString(ConstantPackValues.PackMcMeta())
        )
    }

    private fun saveOverrides(root: File) {
        val overridesResource = object {}.javaClass.getResource("/pack/vanilla_overrides")
        val overridesPath = overridesResource!!.toURI().path
        val sourceDir = File(overridesPath)

        val targetDir = File(Path(root.toString(), "assets", "minecraft").toString())
        targetDir.mkdirs()

        sourceDir.copyRecursively(targetDir, overwrite = true)
    }

    private fun saveTextures(root: File) {
        textures.forEach { (file, parent) ->
            val resourcePath = ArrayList(parent.resourcePath.parts.toList())
            val loc = File(Path(
                root.toString(),
                "assets",
                parent.namespace.name.lowercase(),
                "textures",
                *resourcePath.toTypedArray()
            ).toString())
            loc.mkdirs()

            File(loc, file.name).outputStream().use {
                file.inputStream().copyTo(it)
            }
        }
    }

    private fun saveFonts(root: File) {
        fonts.forEach {
            val resourcePath = ArrayList(it.resource.resourcePath.parts.toList())
            resourcePath[resourcePath.lastIndex] = "${resourcePath[resourcePath.lastIndex]}.json"

            val parent = File(Path(
                root.toString(),
                "assets",
                it.resource.namespace.name.lowercase(),
                "font",
                *resourcePath.dropLast(1).toTypedArray()
            ).toString())
            parent.mkdirs()

            File(parent, resourcePath.last()).writeText(Json.encodeToString(it))
        }
    }
}