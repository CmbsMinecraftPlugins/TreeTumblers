package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.controllers.player.NametagController
import xyz.devcmb.tumblers.util.Format

@Suppress("unused")
@Permission("tumbling.dev")
class NametagCommand {
    @Command("nametag reload [player]")
    fun executeNametag(source: CommandSourceStack, player: Player?) {
        if(player == null) {
            NametagController.refreshAllTags()
        } else {
            NametagController.refreshPlayerTags(player)
        }

        val sender = source.sender
        sender.sendMessage(Format.success("Nametags have been reloaded successfully!"))
    }

    @Command("nametag mode <mode>")
    fun executeMode(source: CommandSourceStack, mode: NametagController.NametagMode) {
        NametagController.currentTagMode = mode

        val sender = source.sender
        sender.sendMessage(Format.success("Nametag mode has been updated successfully!"))
    }

    @Command("nametag remove <player>")
    fun executeRemove(source: CommandSourceStack, player: Player) {
        NametagController.removePlayerTags(player)

        val sender = source.sender
        sender.sendMessage(Format.success("Nametag has been removed successfully!"))
    }
}
