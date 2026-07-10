package xyz.devcmb.pack

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.bukkit.configuration.file.YamlConfiguration
import xyz.devcmb.font.FontProvider
import xyz.devcmb.font.GeneratedFont
import xyz.devcmb.items.GeneratedItem
import xyz.devcmb.models.GeneratedModel
import xyz.devcmb.util.ConstantPackValues
import xyz.devcmb.util.IdentifiedResource
import xyz.devcmb.util.Logger
import xyz.devcmb.util.Namespace
import java.io.File
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.Path
import kotlin.io.path.deleteRecursively
import kotlin.io.path.outputStream
import kotlin.time.Duration.Companion.milliseconds

class GeneratedResourcePack(
    val fonts: Iterable<GeneratedFont>,
    val models: Iterable<GeneratedModel>,
    val items: Iterable<GeneratedItem>,
    val textures: HashSet<Pair<File, IdentifiedResource>>
) {
    val fontTextureIndex: YamlConfiguration = YamlConfiguration()

    @OptIn(ExperimentalPathApi::class)
    fun savePack(locations: HashMap<File, Boolean>) {
        Logger.info("Saving pack...")

        runBlocking(Dispatchers.IO) {
            val tempDir = Files.createTempDirectory("resourcepack")
            val tempZip = Files.createTempFile("pack", ".zip")

            try {
                val tempRoot = tempDir.toFile()

                savePackRoot(tempRoot)
                saveOverrides(tempRoot)
                saveTextures(tempRoot)
                saveFonts(tempRoot)
                saveFontIndex()
                saveModels(tempRoot)
                saveItems(tempRoot)
                saveSounds(tempRoot)

                ZipOutputStream(tempZip.outputStream().buffered()).use { zip ->
                    tempRoot.listFiles()?.forEach { child ->
                        zipRecursively(child.toPath(), tempDir, zip)
                    }
                }

                Logger.success("Pack zipped successfully")

                locations.forEach { (it, zip) ->
                    val packOutput = File(it, "pack${if (zip) ".zip" else ""}")

                    it.mkdirs()

                    if (packOutput.exists()) {
                        while (packOutput.exists()) {
                            Logger.warn("Pack output already exists ${packOutput.canonicalPath}. Attemping to remove.")
                            try {
                                if(packOutput.isDirectory) packOutput.deleteRecursively() else packOutput.delete()
                                delay(200.milliseconds)
                            } catch (_: Exception) {}
                        }
                    }

                    Files.copy(
                        if(zip) tempZip else tempDir,
                        packOutput.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )

                    Logger.success("Pack saved to $packOutput")
                }
            } catch (e: Exception) {
                Logger.error("Failed to save pack assets: ${e.message}")
            } finally {
                try {
                    tempDir.deleteRecursively()
                } catch (_: Exception) {}
            }
        }
    }

    private fun zipRecursively(
        file: Path,
        root: Path,
        zip: ZipOutputStream
    ) {
        if (Files.isDirectory(file)) {
            Files.list(file).use { stream ->
                stream.forEach { child ->
                    zipRecursively(child, root, zip)
                }
            }
        } else {
            val entryName = root.relativize(file).toString().replace('\\', '/')

            zip.putNextEntry(ZipEntry(entryName))
            Files.newInputStream(file).use { it.copyTo(zip) }
            zip.closeEntry()
        }
    }

    private fun savePackRoot(root: File) {
        Logger.info("Saving pack root...")
        val pack = object {}.javaClass.getResource("/pack/pack.png")
        File(root, "pack.png").outputStream().use {
            pack!!.openStream().copyTo(it)
        }

        File(root, "pack.mcmeta").writeText(
            Json.encodeToString(ConstantPackValues.PackMcMeta())
        )

        Logger.success("Saved pack root contents successfully")
    }

    private fun saveOverrides(root: File) {
        Logger.info("Saving pack overrides...")
        val overridesResource = object {}.javaClass.getResource("/pack/vanilla_overrides")
        val overridesPath = overridesResource!!.toURI().path
        val sourceDir = File(overridesPath)

        val targetDir = File(Path(root.toString(), "assets", "minecraft").toString())
        targetDir.mkdirs()

        sourceDir.copyRecursively(targetDir, overwrite = true)
        Logger.success("Saved vanilla overrides successfully")
    }

    private fun saveTextures(root: File) {
        Logger.info("Saving pack textures...")
        textures.forEach { (file, parent) ->
            val resourcePath = ArrayList(parent.resourcePath.parts.toList())
            val loc = File(Path(
                root.toString(),
                "assets",
                parent.namespace.name.lowercase(),
                "textures",
                *resourcePath.dropLast(1).toTypedArray()
            ).toString())
            loc.mkdirs()

            File(loc, resourcePath.last()).outputStream().use {
                file.inputStream().copyTo(it)
            }
            Logger.success("Saved texture ${resourcePath.joinToString("/")} successfully")
        }
        Logger.success("Saved all textures successfully")
    }

    private fun saveFonts(root: File) {
        Logger.info("Saving pack fonts...")
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

            it.providers.forEach { provider ->
                val provider = provider as? FontProvider.BitmapFontProvider ?: return@forEach
                if(provider.chars.size > 1) return@forEach

                var key = "${it.resource.resourcePath.path}.${provider.file.resourcePath.path
                    .substringAfter("font/")
                    .substringAfter("item/")
                    .substringBefore(".png")}"
                if(fontTextureIndex.isSet(key)) {
                    key += "_${provider.ascent}a_${provider.height}h"
                }

                fontTextureIndex.set(
                    key,
                    provider.chars.first()
                )
            }

            File(parent, resourcePath.last()).writeText(Json.encodeToString(it))
            Logger.success("Saved font file ${resourcePath.joinToString("/")} successfully")
        }

        Logger.success("Saved all font files successfully")
    }

    private fun saveFontIndex() {
        Logger.info("Saving font index to server directory...")
        val serverDir = System.getProperty("serverDir")
        val saveDir = File(Path(
            serverDir.toString(),
            "plugins", "TreeTumblers", "font_index.yml"
        ).toString())

        fontTextureIndex.save(saveDir)
        Logger.success("Saved font index successfully")
    }

    private fun saveModels(root: File) {
        Logger.info("Saving models...")
        models.forEach {
            val resourcePath = ArrayList(it.resource.resourcePath.parts.toList())
            resourcePath[resourcePath.lastIndex] = "${resourcePath[resourcePath.lastIndex]}.json"

            val parent = File(Path(
                root.toString(),
                "assets",
                it.resource.namespace.name.lowercase(),
                "models",
                *resourcePath.dropLast(1).toTypedArray()
            ).toString())
            parent.mkdirs()

            File(parent, resourcePath.last()).writeText(it.contents)
            Logger.success("Saved model file ${resourcePath.joinToString("/")} successfully")
        }

        Logger.success("Saved all models successfully")
    }

    private fun saveItems(root: File) {
        Logger.info("Saving items...")
        items.forEach {
            val resourcePath = ArrayList(it.resource.resourcePath.parts.toList())
            resourcePath[resourcePath.lastIndex] = "${resourcePath[resourcePath.lastIndex]}.json"

            val parent = File(Path(
                root.toString(),
                "assets",
                it.resource.namespace.name.lowercase(),
                "items",
                *resourcePath.dropLast(1).toTypedArray()
            ).toString())
            parent.mkdirs()

            File(parent, resourcePath.last()).writeText(it.contents)
            Logger.success("Saved item file ${resourcePath.joinToString("/")} successfully")
        }

        Logger.success("Saved all items successfully")
    }

    private fun saveSounds(root: File) {
        Logger.info("Saving sounds...")

        val soundsFolder = File(javaClass.getResource("/pack/sounds")!!.toURI().path)
        val soundsFile = File(javaClass.getResource("/pack/sounds.json")!!.toURI().path)

        val namespaceRoot = File(Path(
            root.path,
            "assets",
            Namespace.TUMBLING.name.lowercase(),
        ).toString())
        namespaceRoot.mkdirs()

        File(namespaceRoot, "sounds.json").outputStream().use {
            soundsFile.inputStream().copyTo(it)
        }

        val soundsDir = File(namespaceRoot, "sounds")
        soundsDir.mkdirs()
        soundsFolder.copyRecursively(soundsDir)

        Logger.success("Saved all sounds successfully")
    }
}