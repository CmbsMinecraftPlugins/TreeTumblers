package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.flag.Flag
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.WorldController
import xyz.devcmb.tumblers.util.DebugUtil
import java.util.Optional

@Command(name = "world")
class WorldCommand {
    @Execute(name = "create void")
    fun executeWorld(@Context executor: CommandSender, @Arg worldName: String, @Flag("--teleport","-t") teleport: Boolean) {
        val worldController = ControllerDelegate.getController("worldController") as WorldController
        try {
            val world = worldController.createVoidWorld(worldName)
            executor.sendMessage(Component.text("Created void world $worldName successfully!", NamedTextColor.GREEN))

            if(teleport) {
                if(executor !is Player) {
                    executor.sendMessage(Component.text("Only players can teleport to newly created worlds.", NamedTextColor.RED))
                    return
                }

                executor.teleport(Location(world, 0.0, 64.0, 0.0))
            }
        } catch(e: Exception) {
            executor.sendMessage(Component.text("An error occurred while trying to create a void world.", NamedTextColor.RED))
            DebugUtil.severe("Failed to create void world from name: ${e.message ?: "Unknown Error"}")
        }
    }

    @Execute(name = "tp")
    fun teleport(@Context sender: Player, @Arg world: World, @Arg pos: Optional<Location>) {
        val position = pos.orElse(Location(world, 0.0, 128.0, 0.0))
        sender.teleport(Location(world, position.x, position.y, position.z))
    }
}
