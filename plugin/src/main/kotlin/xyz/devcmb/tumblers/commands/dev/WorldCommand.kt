package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.entity.Player
import org.incendo.cloud.annotation.specifier.Quoted
import org.incendo.cloud.annotations.Argument
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Flag
import org.incendo.cloud.annotations.Permission
import org.incendo.cloud.annotations.suggestion.Suggestions
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingWorldException
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.commands.validateGame
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tp
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import kotlin.io.path.Path

@Suppress("unused")
@Permission("tumbling.dev")
class WorldCommand {
    @Command("world create void <worldName>")
    fun executeWorld(source: CommandSourceStack, worldName: String, @Flag("teleport") teleport: Boolean) {
        val executor = source.sender
        try {
            val world = WorldController.createVoidWorld(worldName)
            executor.sendMessage(Format.success("Created void world $worldName successfully!"))

            if(teleport) {
                if(executor !is Player) {
                    executor.sendMessage(Format.error("Only players can teleport to newly created worlds."))
                    return
                }

                executor.tp(Location(world, 0.0, 64.0, 0.0))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to create a void world."))
            DebugUtil.severe("Failed to create void world: ${e.message ?: "Unknown Error"}")
        }
    }

    @Command("world template save <world> <game> <name>")
    fun templateSave(
        source: CommandSourceStack,
        world: World,
        @Argument(suggestions = "registered_games") game: String,
        name: String,
        @Flag("confirm") confirm: Boolean
    ) {
        val executor = source.sender
        val registeredGame = game.validateGame(executor) ?: return

        try {
            if(WorldController.worldFileExists(registeredGame, name) && !confirm) {
                executor.sendMessage(Format.warning("A world with this name already exists! Re-run the command with the --confirm flag to override the existing world!"))
                return
            }

            executor.sendMessage(Format.info("Starting save job..."))
            TreeTumblers.pluginScope.launch {
                WorldController.saveWorld(world, registeredGame, name)
                executor.sendMessage(Format.success("World saved successfully!"))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to save the world."))
            DebugUtil.severe("Failed to save world: ${e.message ?: "Unknown Error"}")
            e.printStackTrace()
        }
    }

    @Command("world hub save")
    fun hubSave(source: CommandSourceStack, @Flag("confirm") confirm: Boolean) {
        val executor = source.sender
        if(!confirm) {
            executor.sendMessage(Format.warning("This operation will overwrite the existing hub world! Re-run with --confirm to execute."))
            return
        }

        val world = Bukkit.getWorld(WorldController.lobbyWorld)
        if(world == null) {
            executor.sendMessage(Format.error("A hub world is not loaded!"))
            return
        }

        try {
            executor.sendMessage(Format.info("Starting save job..."))
            TreeTumblers.pluginScope.launch {
                WorldController.saveWorld(
                    world,
                    File(WorldController.worldRoot, WorldController.lobbyWorld),
                    true
                )
                executor.sendMessage(Format.success("Saved hub world successfully!"))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while saving the hub world."))
            DebugUtil.severe("Failed to save hub: ${e.message ?: "Unknown Error"}")
        }
    }

    @Command("world template load <template>")
    fun loadTemplate(
        source: CommandSourceStack,
        @Quoted @Argument(suggestions = "world_templates") template: String,
        @Flag("teleport") teleport: Boolean
    ) {
        val sender = source.sender

        val templateFile = worldPathToFile(template)
        if(templateFile == null) {
            sender.sendMessage(Format.error("World with specified path does not exist!"))
            return
        }

        val name = templateFile.name
        if(Bukkit.getWorld("temp_$name") != null) {
            sender.sendMessage(Format.error("A temporary world with the name $name already exists!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            try {
                sender.sendMessage(Format.info("Loading template..."))

                val world = WorldController.loadTemplate(Path(templateFile.path), name)
                sender.sendMessage(Format.success("Loaded template world $name successfully!"))

                if(!teleport) return@launch

                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can teleport to loaded worlds."))
                    return@launch
                }

                suspendSync {
                    sender.tp(Location(world, 0.0, 64.0, 0.0))
                }
            } catch(e: Exception) {
                sender.sendMessage(Format.error("An error occurred while trying to load the world."))
                DebugUtil.severe("Failed to load world: ${e.message ?: "Unknown Error"}")
            }
        }
    }

    @Command("world tp <world> [pos]")
    fun teleport(source: CommandSourceStack, world: World, pos: Location?) {
        val sender = source.sender.requirePlayer() ?: return

        val position = pos ?: Location(world, 0.0, 128.0, 0.0)
        sender.tp(Location(world, position.x, position.y, position.z))
        sender.sendMessage(Format.success("Teleported to ${world.name} successfully!"))
    }

    @Command("world migrate <world>")
    @Suppress("UnstableApiUsage")
    fun migrate(
        source: CommandSourceStack,
        @Quoted @Argument(suggestions = "world_templates") world: String
    ) {
        val sender = source.sender

        val worldFile = worldPathToFile(world)
        if(worldFile == null) {
            sender.sendMessage(Format.error("World with specified path does not exist!"))
            return
        }

        if(Files.exists(Path(Bukkit.getServer().levelDirectory.toString(), "dimensions/minecraft/${worldFile.name}"))) {
            sender.sendMessage(Format.error("You cannot migrate a world while it is loaded!"))
            return
        }

        if(!Files.exists(Path(worldFile.toString(), "level.dat"))) {
            sender.sendMessage(Format.warning("World is already migrated"))
            return
        }

        sender.sendMessage(Format.info("Starting migration..."))
        TreeTumblers.pluginScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FileUtils.copyDirectory(worldFile, File(Bukkit.getWorldContainer(), worldFile.name))
                }

                delay(12000)

                val bukkitWorld = suspendSync {
                    Bukkit.createWorld(WorldCreator(worldFile.name))
                        ?: throw TumblingWorldException("Failed to load world ${worldFile.name}")
                }

                delay(1000)

                suspendSync {
                    Bukkit.unloadWorld(bukkitWorld, false)
                }

                delay(3000)

                withContext(Dispatchers.IO) {
                    val isVoid = Files.exists(Path(worldFile.toString(), "void.txt"))
                    WorldController.deleteDir(worldFile)
                    delay(5000)
                    val from = File(WorldController.getDimensions(), worldFile.name)
                    val idFile = File(from, "data/paper/metadata.dat")
                    if (idFile.exists()) {
                        idFile.delete()
                    }

                    FileUtils.copyDirectory(from, worldFile)
                    delay(3000)
                    WorldController.deleteDir(from)
                    if(isVoid) {
                        Files.write(
                            File(worldFile, "void.txt").toPath(),
                            listOf(""),
                            StandardCharsets.UTF_8
                        )
                    }
                }

                sender.sendMessage(Format.success("Migration success!"))
            } catch (e: Exception) {
                DebugUtil.severe("Failed to migrate world ${worldFile.name}: ${e.message} (${e.cause})")
                sender.sendMessage(Format.error("An error occurred while migrating the world"))
            }
        }
    }

    private fun worldPathToFile(path: String): File? {
        val validPaths = getPaths()
        if(!validPaths.contains(path)) {
            return null
        }

        val parentDir = File(WorldController.worldRoot)
        val file = File(parentDir, path)
        return if(file.exists()) file else null
    }

    private fun getPaths(): ArrayList<String> {
        val suggestions: ArrayList<String> = ArrayList()

        fun scanWorldsFolder(parent: File, pathString: String) {
            parent.listFiles()!!.forEach {
                if(!it.isDirectory) return@forEach

                if(File(it, "paper-world.yml").exists()) {
                    suggestions.add("$pathString${it.name}")
                } else {
                    scanWorldsFolder(it, "$pathString${it.name}/")
                }
            }
        }

        scanWorldsFolder(
            File(WorldController.worldRoot),
            ""
        )

        return suggestions
    }

    @Suggestions("world_templates")
    fun suggestWorldTemplates(): List<String> {
        return getPaths().map { "\"$it\"" }
    }

    @Suggestions("registered_games")
    fun suggestRegisteredGames(): List<String> {
        return GameController.games.map { it.data.id }
    }
}
