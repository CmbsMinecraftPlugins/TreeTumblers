package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.controllers.WorldController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import java.util.Optional
import kotlin.io.path.Path
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Command(name = "world")
class WorldCommand {
    val worldController: WorldController by lazy {
        ControllerDelegate.getController("worldController") as WorldController
    }

    @Execute(name = "create void")
    fun executeWorld(@Context executor: CommandSender, @Arg worldName: String, @Flag("--teleport","-t") teleport: Boolean) {
        try {
            val world = worldController.createVoidWorld(worldName)
            executor.sendMessage(Format.success("Created void world $worldName successfully!"))

            if(teleport) {
                if(executor !is Player) {
                    executor.sendMessage(Format.error("Only players can teleport to newly created worlds."))
                    return
                }

                executor.teleport(Location(world, 0.0, 64.0, 0.0))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to create a void world."))
            DebugUtil.severe("Failed to create void world: ${e.message ?: "Unknown Error"}")
        }
    }

    @Execute(name = "template save")
    fun templateSave(@Context executor: CommandSender, @Arg world: World, @Arg game: GameController.Game, @Arg name: Optional<String>) {
        try {
            executor.sendMessage(Format.info("Starting save job..."))
            TreeTumblers.pluginScope.launch {
                worldController.saveWorld(world, game, name.getOrNull())
                executor.sendMessage(Component.text("World saved successfully!", NamedTextColor.GREEN))
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to save the world."))
            DebugUtil.severe("Failed to save world: ${e.message ?: "Unknown Error"}")
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
        try {
            executor.sendMessage(Format.info("Loading template..."))
            TreeTumblers.pluginScope.launch {
                val world = worldController.loadTemplate(Path(template.file.path), name)
                executor.sendMessage(Format.success("Loaded template world $name successfully!"))

                if(!teleport) return@launch

                if(executor !is Player) {
                    executor.sendMessage(Format.error("Only players can teleport to loaded worlds."))
                    return@launch
                }

                suspendSync {
                    executor.teleport(Location(world, 0.0, 64.0, 0.0))
                }
            }
        } catch(e: Exception) {
            executor.sendMessage(Format.error("An error occurred while trying to load the world."))
            DebugUtil.severe("Failed to load world: ${e.message ?: "Unknown Error"}")
        }
    }

    @Execute(name = "tp")
    fun teleport(@Context sender: Player, @Arg world: World, @Arg pos: Optional<Location>) {
        val position = pos.orElse(Location(world, 0.0, 128.0, 0.0))
        sender.teleport(Location(world, position.x, position.y, position.z))
    }
}
