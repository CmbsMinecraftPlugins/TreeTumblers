package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import org.bukkit.Location
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.controllers.player.NoxesiumController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.toCenterXZLocation

@Suppress("unused")
@Permission("tumbling.dev")
class QibCommand {

    @Command("qib spawn <type> [location]")
    fun spawnQib(source: CommandSourceStack, type: NoxesiumController.QibType, location: Location?) {
        val sender = source.sender
        if(location == null && sender !is Player) {
            sender.sendMessage(Format.error("Only players can use this command if the location argument is not provided!"))
            return
        }

        val location = location ?: (sender as Player).location.clone().add(0.0,-1.0,0.0)
        type.spawn(location.toCenterXZLocation())
        sender.sendMessage(Format.success("Spawned a QIB of type ${type.name.lowercase()} successfully!"))
    }

}
