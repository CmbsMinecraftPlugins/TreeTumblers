package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.player.SpectatorController
import xyz.devcmb.tumblers.util.Format
import java.util.Optional

@Command(name = "spectate")
@Permission("tumbling.dev")
class SpectateCommand {
    @Execute(name = "enable")
    fun enableSpectate(@Context player: Player, @Arg target: Optional<Player>) {
        val target = target.orElse(player) as Player
        if(SpectatorController.spectators.containsKey(target)) {
            player.sendMessage(Format.warning("Nothing changed, player is already a spectator"))
            return
        }

        SpectatorController.makeSpectator(target, true)
        player.sendMessage(Format.success("Made player spectate successfully!"))
    }

    @Execute(name = "disable")
    fun disableSpectate(@Context player: Player, @Arg target: Optional<Player>) {
        val target = target.orElse(player) as Player
        if(!SpectatorController.spectators.containsKey(target)) {
            player.sendMessage(Format.warning("Nothing changed, player is not a spectator"))
            return
        }

        SpectatorController.unSpectate(target)
        player.sendMessage(Format.success("Took a player out of spectate successfully!"))
    }
}
