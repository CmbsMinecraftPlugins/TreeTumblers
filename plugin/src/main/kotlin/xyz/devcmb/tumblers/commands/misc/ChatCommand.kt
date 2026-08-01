package xyz.devcmb.tumblers.commands.misc

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.player.ChatController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName

@Command(name = "chat")
class ChatCommand {
    @Execute
    fun executeChat(@Context executor: Player, @Arg channel: ChatController.ChatChannel) {
        ChatController.channels[executor] = channel
        executor.sendMessage(Format.info(Format.mm(
            "You are now in the <channel> channel.",
            Placeholder.parsed("channel", "<color:${channel.color.asHexString()}>${channel.channelName}</color>")
        )))
    }

    @Execute(name = "announcement")
    @Permission("tumbling.dev", "tumbling.organizer")
    fun executeAnnouncement(@Context executor: CommandSender, @Arg("message") message: String) {
        Bukkit.broadcast(Format.mm("<yellow><line:30></yellow><white><br><br><!st>" +
                "<glyph:icon/warning/yellow> <yellow>Announcement</yellow><br>" +
                "${if(executor is Player) "<player>:" else ""} $message</white><br><br>" +
                "<yellow><line:30></yellow>",
            Placeholder.component("player", (executor as? Player)?.formattedName ?: Component.empty())
        ))
    }
}
