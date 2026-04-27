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
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.server.ServerLoadEvent
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.WorldCreationException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.engine.GameBase.Companion.lobbyPosition
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates
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

        @field:Configurable("lobby.world")
        var lobbyWorld: String = "hub"
    }

    override fun init() {
        // do it both on cleanup and start so it cleans up regardless of if the server gracefully shut down
        cleanupTempWorlds()
    }

    override fun cleanup() = cleanupTempWorlds()

    fun cleanupTempWorlds() {
        Bukkit.getWorldContainer().listFiles().forEach { file ->
            if(file.isDirectory && (file.name.contains("temp_") || file.name == lobbyWorld)) {
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

        val worldsFolder = Path.of(
            worldRoot,
            TreeTumblers.plugin.config.getString("${game.configRoot}.worlds_folder")
        )

        saveWorld(world, File(worldsFolder.toString(), name))
    }

    suspend fun saveWorld(world: World, path: File, reload: Boolean = false) {
        val worldFolder = world.worldFolder

        suspendSync {
            world.players.forEach {
                val hub = Bukkit.getWorld(lobbyWorld)
                val location = if(world.name == lobbyWorld || hub == null) {
                    Location(Bukkit.getWorld("world")!!, 0.0, 127.0, 0.0)
                } else {
                    lobbyPosition.unpackCoordinates(hub)
                }

                it.teleport(location)
            }
            Bukkit.unloadWorld(world, true)
        }

        // Seems to be a good time to wait for all the IO operations to stop, fix if not
        delay(3000)

        withContext(Dispatchers.IO) {
            val destination = path
            FileUtils.copyDirectory(worldFolder, destination)

            val idFile = File(destination, "uid.dat")
            if(idFile.exists()) {
                idFile.delete()
            }

            if(reload) {
                suspendSync {
                    Bukkit.createWorld(
                        WorldCreator(world.name)
                        .generator(world.generator)
                    )
                }
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

        deleteDir(file)
    }

    // my savior
    // https://www.spigotmc.org/threads/cant-delete-world-folder-after-unloading-it.314857/
    private fun deleteDir(file2: File) {
        val contents = file2.listFiles()
        if (contents != null) {
            for (f in contents) {
                deleteDir(f)
            }
        }
        file2.delete()
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun onServerLoad(event: ServerLoadEvent) {
        val source = File(worldRoot, lobbyWorld)
        val name = lobbyWorld
        val destination = File(Bukkit.getWorldContainer(), name)

        FileUtils.copyDirectory(
            source,
            destination
        )

        var worldCreator = WorldCreator(name)
        worldCreator = worldCreator.generator(MiscUtils.VoidGenerator)

        Bukkit.createWorld(worldCreator)!!
    }

    data class LoadableTemplate(val file: File)
}