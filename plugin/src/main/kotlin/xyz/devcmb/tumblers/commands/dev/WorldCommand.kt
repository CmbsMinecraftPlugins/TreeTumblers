package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import dev.rollczi.litecommands.annotations.permission.Permission
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.apache.commons.io.FileUtils
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.WorldCreator
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingWorldException
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tp
import java.io.File
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.util.Optional
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Command(name = "world")
@Permission("tumbling.dev")
class WorldCommand {
    @Execute(name = "create void")
    fun executeWorld(@Context executor: CommandSender, @Arg("world name") worldName: String, @Flag("--teleport","-t") teleport: Boolean) {
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

    @Execute(name = "template save")
    fun templateSave(
        @Context executor: CommandSender,
        @Arg("world") world: World,
        @Arg("game") game: GameController.RegisteredGame,
        @Arg("name") name: Optional<String>,
        @Flag("--confirm","-c") confirm: Boolean
    ) {
        try {
            if(WorldController.worldFileExists(game, name.getOrElse { world.name }) && !confirm) {
                executor.sendMessage(Format.warning("A world with this name already exists! Re-run the command with the --confirm flag to override the existing world!"))
                return
            }

            executor.sendMessage(Format.info("Starting save job..."))
            TreeTumblers.pluginScope.launch {
                WorldController.saveWorld(world, game, name.getOrNull())
                executor.sendMessage(Format.success("World saved successfully!"))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to save the world."))
            DebugUtil.severe("Failed to save world: ${e.message ?: "Unknown Error"}")
            e.printStackTrace()
        }
    }

    @Execute(name = "hub save")
    fun hubSave(@Context executor: CommandSender, @Flag("--confirm","-c") confirm: Boolean) {
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

    @Execute(name = "template load")
    fun loadTemplate(
        @Context executor: CommandSender,
        @Arg template: WorldController.LoadableTemplate,
        @Arg name: Optional<String>,
        @Flag("--teleport","-t") teleport: Boolean
    ) {
        val name = name.getOrElse { template.file.name }
        if(Bukkit.getWorld("temp_$name") != null) {
            executor.sendMessage(Format.error("A temporary world with the name $name already exists!"))
            return
        }

        TreeTumblers.pluginScope.launch {
            try {
                executor.sendMessage(Format.info("Loading template..."))

                val world = WorldController.loadTemplate(Path(template.file.path), name)
                executor.sendMessage(Format.success("Loaded template world $name successfully!"))

                if(!teleport) return@launch

                if(executor !is Player) {
                    executor.sendMessage(Format.error("Only players can teleport to loaded worlds."))
                    return@launch
                }

                suspendSync {
                    executor.tp(Location(world, 0.0, 64.0, 0.0))
                }
            } catch(e: Exception) {
                executor.sendMessage(Format.error("An error occurred while trying to load the world."))
                DebugUtil.severe("Failed to load world: ${e.message ?: "Unknown Error"}")
            }
        }
    }

    @Execute(name = "tp")
    fun teleport(@Context sender: Player, @Arg world: World, @Arg pos: Optional<Location>) {
        val position = pos.getOrElse { Location(world, 0.0, 128.0, 0.0) }
        sender.tp(Location(world, position.x, position.y, position.z))
        sender.sendMessage(Format.success("Teleported to ${world.name} successfully!"))
    }

    @Execute(name = "migrate")
    @Suppress("UnstableApiUsage")
    fun migrate(@Context sender: CommandSender, @Arg world: WorldController.LoadableTemplate) {
        if(Files.exists(Path(Bukkit.getServer().levelDirectory.toString(), "dimensions/minecraft/${world.file.name}"))) {
            sender.sendMessage(Format.error("You cannot migrate a world while it is loaded!"))
            return
        }

        if(!Files.exists(Path(world.file.toString(), "level.dat"))) {
            sender.sendMessage(Format.warning("World is already migrated"))
            return
        }

        sender.sendMessage(Format.info("Starting migration..."))
        TreeTumblers.pluginScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    FileUtils.copyDirectory(world.file, File(Bukkit.getWorldContainer(), world.file.name))
                }

                delay(12000)

                val bukkitWorld = suspendSync {
                    Bukkit.createWorld(WorldCreator(world.file.name))
                        ?: throw TumblingWorldException("Failed to load world ${world.file.name}")
                }

                delay(1000)

                suspendSync {
                    Bukkit.unloadWorld(bukkitWorld, false)
                }

                delay(3000)

                withContext(Dispatchers.IO) {
                    val isVoid = Files.exists(Path(world.file.toString(), "void.txt"))
                    WorldController.deleteDir(world.file)
                    delay(5000)
                    val from = File(WorldController.getDimensions(), world.file.name)
                    val idFile = File(from, "data/paper/metadata.dat")
                    if (idFile.exists()) {
                        idFile.delete()
                    }

                    FileUtils.copyDirectory(from, world.file)
                    delay(3000)
                    WorldController.deleteDir(from)
                    if(isVoid) {
                        Files.write(
                            File(world.file, "void.txt").toPath(),
                            listOf(""),
                            StandardCharsets.UTF_8
                        )
                    }
                }

                sender.sendMessage(Format.success("Migration success!"))
            } catch (e: Exception) {
                DebugUtil.severe("Failed to migrate world ${world.file.name}: ${e.message} (${e.cause})")
                sender.sendMessage(Format.error("An error occurred while migrating the world"))
            }
        }
    }
}
