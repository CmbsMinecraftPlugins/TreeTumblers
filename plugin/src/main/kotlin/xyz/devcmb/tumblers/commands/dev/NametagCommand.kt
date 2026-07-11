package xyz.devcmb.tumblers.commands.dev
import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.player.NametagController
import xyz.devcmb.tumblers.util.Format
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "nametag")
@Permission("tumbling.dev")
class NametagCommand {
    @Execute(name = "reload")
    fun executeNametag(@Context executor: CommandSender, @Arg player: Optional<Player>) {
        val player = player.getOrNull()
        if(player == null) {
            NametagController.refreshAllTags()
        } else {
            NametagController.refreshPlayerTags(player)
        }

        executor.sendMessage(Format.success("Nametags have been reloaded successfully!"))
    }

    @Execute(name = "mode")
    fun executeMode(@Context executor: CommandSender, @Arg("mode") mode: NametagController.NametagMode) {
        NametagController.currentTagMode = mode
        executor.sendMessage(Format.success("Nametag mode has been updated successfully!"))
    }

    @Execute(name = "remove")
    fun executeRemove(@Context executor: CommandSender, @Arg("player") player: Player) {
        NametagController.removePlayerTags(player)
        executor.sendMessage(Format.success("Nametag has been removed successfully!"))
    }
}
