package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.NoxesiumController
import xyz.devcmb.tumblers.util.Format
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "qib")
@Permission("tumbling.dev")
class QibCommand {

    @Execute(name = "spawn")
    fun spawnQib(@Context player: Player, @Arg type: NoxesiumController.QibType, @Arg location: Optional<Location>) {
        val location = location.getOrNull() ?: player.location.clone().add(0.0,-1.0,0.0)
        type.spawn(location)
        player.sendMessage(Format.success("Spawned a QIB of type ${type.name.lowercase()} successfully!"))
    }

}
