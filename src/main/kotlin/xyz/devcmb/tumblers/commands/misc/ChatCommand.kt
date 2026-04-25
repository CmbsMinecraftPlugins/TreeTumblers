package xyz.devcmb.tumblers.commands.misc

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.util.Format

@Command(name = "chat")
class ChatCommand {

    val playerController: PlayerController by lazy {
        ControllerDelegate.getController<PlayerController>()
    }

    @Execute
    fun executeChat(@Context executor: Player, @Arg channel: PlayerController.ChatChannel) {
        playerController.channels.put(executor, channel)
        executor.sendMessage(Format.info(Format.mm(
            "You are now in the <channel> channel.",
            Placeholder.component("channel", Format.mm("<color:${channel.color.asHexString()}>${channel.channelName}</color>"))
        )))
    }

}
