package xyz.devcmb.tumblers.commands.dev
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.util.Format
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "nametag")
@Permission("tumbling.dev")
class NametagCommand {
    val playerController: PlayerController by ControllerRegistry.controller()

    @Execute(name = "reload")
    fun executeNametag(@Context executor: CommandSender, @Arg player: Optional<Player>) {
        val player = player.getOrNull()
        if(player == null) {
            playerController.reloadNametags()
        } else {
            playerController.reloadNametag(player)
        }

        executor.sendMessage(Format.success("Nametags have been reloaded successfully!"))
    }

    @Execute(name = "mode")
    fun executeMode(@Context executor: CommandSender, @Arg("mode") mode: PlayerController.NametagMode) {
        playerController.currentNametagMode = mode
        executor.sendMessage(Format.success("Nametag mode has been updated successfully!"))
    }

    @Execute(name = "remove")
    fun executeRemove(@Context executor: CommandSender, @Arg("player") player: Player) {
        val tag = playerController.nameTags[player]
        if(tag == null) {
            executor.sendMessage(Format.warning("Nothing to remove, player does not have a nametag."))
            return
        }

        tag.remove()
        playerController.nameTags.remove(player)
        executor.sendMessage(Format.success("Nametag has been removed successfully!"))
    }
}
