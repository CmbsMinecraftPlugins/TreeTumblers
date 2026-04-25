package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.SpectatorController
import xyz.devcmb.tumblers.util.Format
import java.util.Optional

@Command(name = "spectate")
class SpectateCommand {
    val spectatorController by lazy {
        ControllerDelegate.getController<SpectatorController>()
    }

    @Execute(name = "enable")
    fun enableSpectate(@Context player: Player, @Arg target: Optional<Player>) {
        val target = target.orElse(player)
        if(spectatorController.spectators.containsKey(target)) {
            player.sendMessage(Format.warning("Nothing changed, player is already a spectator"))
            return
        }

        spectatorController.makeSpectator(target, true)
        player.sendMessage(Format.success("Made player spectate successfully!"))
    }

    @Execute(name = "disable")
    fun disableSpectate(@Context player: Player, @Arg target: Optional<Player>) {
        val target = target.orElse(player)
        if(!spectatorController.spectators.containsKey(target)) {
            player.sendMessage(Format.warning("Nothing changed, player is not a spectator"))
            return
        }

        spectatorController.unSpectate(target)
        player.sendMessage(Format.success("Took a player out of spectate successfully!"))
    }
}
