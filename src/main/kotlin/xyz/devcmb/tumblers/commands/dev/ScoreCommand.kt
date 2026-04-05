package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.util.Optional
import kotlin.jvm.optionals.getOrElse
import kotlin.jvm.optionals.getOrNull

@Command(name = "score")
@Permission("tumbling.dev")
class ScoreCommand {
    val gameController: GameController by lazy {
        ControllerDelegate.getController("gameController") as GameController
    }

    @Execute(name = "event kill")
    fun kill(@Context player: Player, @Arg("killed") killed: Optional<Player>) {
        val activeGame = gameController.activeGame
        if(activeGame == null) {
            player.sendMessage(Format.error("This command can only be used when a game is active!"))
            return
        }

        val debugToolkit = activeGame.debugToolkit
        if(debugToolkit == null){
            player.sendMessage(Format.error("Cannot invoke a debug action on a game without a debug toolkit!"))
            return
        }

        debugToolkit.killEvent(player, killed.getOrNull())
        player.sendMessage(Format.success("Sent a kill signal successfully!"))
    }

    @Execute(name = "event death")
    fun death(@Context player: Player, @Arg("killed") killed: Optional<Player>) {
        val activeGame = gameController.activeGame
        if(activeGame == null) {
            player.sendMessage(Format.error("This command can only be used when a game is active!"))
            return
        }

        val debugToolkit = activeGame.debugToolkit
        if(debugToolkit == null){
            player.sendMessage(Format.error("Cannot invoke a debug action on a game without a debug toolkit!"))
            return
        }

        debugToolkit.deathEvent(killed.getOrElse { player })
        player.sendMessage(Format.success("Sent a death signal successfully!"))
    }

    @Execute(name = "view")
    fun view(@Context sender: CommandSender, @Arg("player") player: Player) {
        val tumblingPlayer = player.tumblingPlayer
        sender.sendMessage(
            Component.empty()
                .append(Format.formatPlayerName(tumblingPlayer))
                .append(Component.text(" - ", NamedTextColor.GRAY))
                .append(Component.text(tumblingPlayer.score, NamedTextColor.GOLD))
        )
    }
}
