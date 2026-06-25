package xyz.devcmb.tumblers.commands.games

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import org.bukkit.command.CommandSender
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.Format

@Command(name = "badge")
class BadgeCommand {
    @Execute(name = "grant")
    fun executeBadge(@Context executor: CommandSender, @Arg player: TumblingPlayer, @Arg badge: BadgeController.Badge) {
        BadgeController.grantBadge(player, badge)
        executor.sendMessage(Format.success(Format.mm("Granted the ${badge.name.lowercase()} badge to <player:${player.uuid}> successfully!")))
    }
}
