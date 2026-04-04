package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.World
import org.bukkit.WorldCreator
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.WorldCreationException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Controller("worldController", Controller.Priority.MEDIUM)
class WorldController : IController {
    companion object {
        @field:Configurable("templates.world_root")
        var worldRoot: String = "&/templates/worlds"
            get() {
                return field
                    .replace("&", TreeTumblers.plugin.dataFolder.path.toString())
            }
    }

    override fun init() {
        // do it both on cleanup and start so it cleans up regardless of if the server gracefully shut down
        cleanupTempWorlds()
    }

    override fun cleanup() = cleanupTempWorlds()

    fun cleanupTempWorlds() {
        Bukkit.getWorldContainer().listFiles().forEach { file ->
            if(file.isDirectory && file.name.contains("temp_")) {
                if(Bukkit.getWorld(file.name) !== null) {
                    Bukkit.unloadWorld(file.name, false)
                }

                // my savior
                // https://www.spigotmc.org/threads/cant-delete-world-folder-after-unloading-it.314857/
                fun deleteDir(file2: File) {
                    val contents = file2.listFiles()
                    if (contents != null) {
                        for (f in contents) {
                            deleteDir(f)
                        }
                    }
                    file2.delete()
                }

                deleteDir(file)
            }
        }
    }

    fun createVoidWorld(worldName: String): World {
        val world = Bukkit.createWorld(WorldCreator(worldName)
            .generator(MiscUtils.VoidGenerator))!!

        world.getBlockAt(0, 64, 0).type = Material.STONE

        val file = File(Bukkit.getWorldContainer(), worldName)
        if(!file.exists() || !file.isDirectory) {
            Bukkit.unloadWorld(worldName, false)
            throw WorldCreationException("World does not have a world folder in the bukkit world container")
        }

        Files.write(
            File(file, "void.txt").toPath(),
            listOf(""),
            StandardCharsets.UTF_8
        )

        return world
    }

    suspend fun loadTemplate(path: Path, name: String): World {
        val name = "temp_$name"
        withContext(Dispatchers.IO) {
            FileUtils.copyDirectory(
                File(path.toString()),
                File(Bukkit.getWorldContainer(), name)
            )
        }

        return suspendSync {
            var worldCreator = WorldCreator(name)
            if(File(path.toString(), "void.txt").exists()) {
                worldCreator = worldCreator.generator(MiscUtils.VoidGenerator)
            }

            Bukkit.createWorld(worldCreator)!!
        }
    }

    suspend fun saveWorld(world: World, game: GameController.Game, name: String? = null) {
        val name = name ?: world.name
        val game = game.getTemplate()
        val worldFolder = world.worldFolder

        suspendSync {
            world.players.forEach {
                // TODO: teleport to some lobby maybe
                it.teleport(Location(
                    Bukkit.getWorld("world")!!,
                    0.0,
                    128.0,
                    0.0
                ))
            }
            Bukkit.unloadWorld(world, true)
        }

        // Seems to be a good time to wait for all the IO operations to stop, fix if not
        delay(3000)

        val worldsFolder = Path.of(
            worldRoot,
            TreeTumblers.plugin.config.getString("${game.configRoot}.worlds_folder")
        )

        withContext(Dispatchers.IO) {
            val destination = File(worldsFolder.toString(), name)
            FileUtils.copyDirectory(worldFolder, destination)

            val idFile = File(destination, "uid.dat")
            if(idFile.exists()) {
                idFile.delete()
            }
        }
    }

    fun worldFileExists(game: GameController.Game, name: String): Boolean {
        val game = game.getTemplate()
        val worldsFolder = File(
            worldRoot,
            TreeTumblers.plugin.config.getString("${game.configRoot}.worlds_folder")!!
        )

        return File(worldsFolder, name).exists()
    }

    suspend fun cleanupWorld(world: World) = withContext(Dispatchers.IO) {
        val file = File(Bukkit.getWorldContainer(), world.name)
        suspendSync {
            Bukkit.unloadWorld(world, false)
        }

        // io halt
        delay(3000)

        // my savior
        // https://www.spigotmc.org/threads/cant-delete-world-folder-after-unloading-it.314857/
        fun deleteDir(file2: File) {
            val contents = file2.listFiles()
            if (contents != null) {
                for (f in contents) {
                    deleteDir(f)
                }
            }
            file2.delete()
        }

        deleteDir(file)
    }

    data class LoadableTemplate(val file: File)
}