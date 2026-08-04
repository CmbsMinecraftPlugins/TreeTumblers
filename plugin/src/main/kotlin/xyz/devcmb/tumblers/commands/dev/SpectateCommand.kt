package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.dev")
class SpectateCommand {
    @Command("spectate enable [target]")
    fun enableSpectate(source: CommandSourceStack, target: Player?) {
        val sender = source.sender
        if(target == null && sender !is Player) {
            sender.sendMessage(Format.error("Only players can use this command if the target argument is not provided!"))
            return
        }

        val target = target ?: (sender as Player)
        if(SpectatorController.spectators.contains(target)) {
            sender.sendMessage(Format.warning("Nothing changed, player is already a spectator"))
            return
        }

        SpectatorController.makeSpectator(target)
        sender.sendMessage(Format.success("Made player spectate successfully!"))
    }

    @Command("spectate disable [target]")
    fun disableSpectate(source: CommandSourceStack, target: Player?) {
        val sender = source.sender
        if(target == null && sender !is Player) {
            sender.sendMessage(Format.error("Only players can use this command if the target argument is not provided!"))
            return
        }

        val target = target ?: (sender as Player)
        if(!SpectatorController.spectators.contains(target)) {
            sender.sendMessage(Format.warning("Nothing changed, player is not a spectator"))
            return
        }

        SpectatorController.unSpectate(target)
        sender.sendMessage(Format.success("Took a player out of spectate successfully!"))
    }
}
