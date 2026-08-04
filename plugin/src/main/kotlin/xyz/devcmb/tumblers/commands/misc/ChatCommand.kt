package xyz.devcmb.tumblers.commands.misc

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.commands.requirePlayer
import xyz.devcmb.tumblers.controllers.player.ChatController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName

@Suppress("unused")
class ChatCommand {
    @Command("chat <channel>")
    fun executeChat(source: CommandSourceStack, channel: ChatController.ChatChannel) {
        val player = source.sender.requirePlayer() ?: return
        ChatController.channels[player] = channel
        player.sendMessage(Format.info(Format.mm(
            "You are now in the <channel> channel.",
            Placeholder.parsed("channel", "<color:${channel.color.asHexString()}>${channel.channelName}</color>")
        )))
    }

    @Command("chat announcement <message>")
    @Permission(value = ["tumbling.dev", "tumbling.organizer"], mode = Permission.Mode.ANY_OF)
    fun executeAnnouncement(source: CommandSourceStack, message: String) {
        val sender = source.sender
        Bukkit.broadcast(Format.mm("<yellow><line:30></yellow><white><br><br><!st>" +
                "<glyph:icon/warning/yellow> <yellow>Announcement</yellow><br>" +
                "${if(sender is Player) "<player>:" else ""} $message</white><br><br>" +
                "<yellow><line:30></yellow>",
            Placeholder.component("player", (sender as? Player)?.formattedName ?: Component.empty())
        ))
    }
}
