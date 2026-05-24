package xyz.devcmb.tumblers.controllers.server

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Entity
import org.bukkit.persistence.PersistentDataHolder
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingWorldException
import xyz.devcmb.tumblers.WorldCreationException
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.ControllerBase
import xyz.devcmb.tumblers.controllers.event.HubController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.VoidGenerator
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tp
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path

@Controller(Controller.Priority.HIGH)
class WorldController : ControllerBase() {
    companion object {
        val worldRoot: String = configurable("templates.world_root")
            get() {
                return field
                    .replace("&", TreeTumblers.plugin.dataFolder.path.toString())
            }

        val lobbyWorld: String = configurable("lobby.world")
        val temporaryEntityKey: NamespacedKey = NamespacedKey(TreeTumblers.NAMESPACE, "temp_entity")
    }

    val hubController: HubController by controller()

    override fun init() {
        // do it both on cleanup and start so it cleans up regardless of if the server gracefully shut down
        cleanupTempWorlds()
    }

    override fun cleanup() = cleanupTempWorlds()

    fun cleanupTempWorlds() {
        getDimensions().listFiles().forEach { file ->
            DebugUtil.info("cleanup temporary world ${file.absolutePath}")
            if(file.isDirectory && (file.name.contains("temp_") || file.name == lobbyWorld)) {
                DebugUtil.info("Cleaning up ${file.absolutePath}")
                if(Bukkit.getWorld(file.name) !== null) {
                    Bukkit.unloadWorld(file.name, false)
                }

                deleteDir(file)
            }
        }
    }

    fun createVoidWorld(worldName: String): World {
        val world = Bukkit.createWorld(
            WorldCreator(worldName)
            .generator(VoidGenerator))!!

        world.getBlockAt(0, 64, 0).type = Material.STONE

        val file = File(getDimensions(), worldName)
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
        val parent =
            if(Files.exists(Path.of(path.toString(), name, "level.dat"))) Bukkit.getWorldContainer()
            else getDimensions()

        val name = "temp_$name"
        withContext(Dispatchers.IO) {
            FileUtils.copyDirectory(
                File(path.toString()),
                File(parent, name)
            )
        }

        return suspendSync {
            var worldCreator = WorldCreator(name)
            if (File(path.toString(), "void.txt").exists()) {
                worldCreator = worldCreator.generator(VoidGenerator)
            }

            Bukkit.createWorld(worldCreator) ?: throw TumblingWorldException("Failed to load template world $name")
        }
    }

    suspend fun saveWorld(world: World, game: GameController.RegisteredGame, name: String? = null) {
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
                val location = if (world.name == lobbyWorld || hub == null) {
                    Location(Bukkit.getWorld("world")!!, 0.0, 127.0, 0.0)
                } else {
                    hubController.getLobbyPosition()
                }

                it.tp(location)
            }
            Bukkit.unloadWorld(world, true)
        }

        // Seems to be a good time to wait for all the IO operations to stop, fix if not
        delay(3000)

        withContext(Dispatchers.IO) {
            FileUtils.copyDirectory(worldFolder, path)

            val idFile = File(path, "data/paper/metadata.dat")
            if (idFile.exists()) {
                idFile.delete()
            }

            if (reload) {
                suspendSync {
                    Bukkit.createWorld(
                        WorldCreator(world.name)
                            .generator(world.generator)
                    )
                }
            }
        }
    }

    fun worldFileExists(game: GameController.RegisteredGame, name: String): Boolean {
        val game = game.getTemplate()
        val worldsFolder = File(
            worldRoot,
            TreeTumblers.plugin.config.getString("${game.configRoot}.worlds_folder")!!
        )

        return File(worldsFolder, name).exists()
    }

    suspend fun cleanupWorld(world: World) = withContext(Dispatchers.IO) {
        val file = File(getDimensions(), world.name)
        suspendSync {
            Bukkit.unloadWorld(world, false)
        }

        // io halt
        delay(3000)

        deleteDir(file)
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

    override fun serverLoad() {
        val source = File(worldRoot, lobbyWorld)
        val name = lobbyWorld
        val parent =
            if(Files.exists(Path.of(source.toString(), "level.dat"))) Bukkit.getWorldContainer()
            else getDimensions()

        val destination = File(parent, name)

        FileUtils.copyDirectory(
            source,
            destination
        )

        var worldCreator = WorldCreator(name)
        worldCreator = worldCreator.generator(VoidGenerator)

        val world = Bukkit.createWorld(worldCreator)!!
        world.entities
            .filter { it is PersistentDataHolder && it.persistentDataContainer.has(temporaryEntityKey) }
            .forEach(Entity::remove)
    }

    @Suppress("UnstableApiUsage")
    fun getDimensions(): File {
        return File(Bukkit.getServer().levelDirectory.toString(), "dimensions/minecraft")
    }

    data class LoadableTemplate(val file: File)
}