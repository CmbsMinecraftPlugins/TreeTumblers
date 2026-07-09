package xyz.devcmb.tumblers.commands.dev

import dev.rollczi.litecommands.annotations.argument.Arg
import dev.rollczi.litecommands.annotations.command.Command
import dev.rollczi.litecommands.annotations.context.Context
import dev.rollczi.litecommands.annotations.execute.Execute
import dev.rollczi.litecommands.annotations.permission.Permission
import net.kyori.adventure.resource.ResourcePackInfo
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.events.LoggedOnTumblingPlayerReadyEvent
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import java.net.URI
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@Command(name = "pack")
@Permission("tumbling.dev")
class PackCommand {
    val packURL: String = configurable("pack.url")
    val packHash: String = configurable("pack.hash")

    val resourcePackInfo: ResourcePackInfo = ResourcePackInfo.resourcePackInfo()
        .uri(URI(packURL))
        .hash(packHash)
        .build()

    @Execute(name = "reload")
    fun reloadPacks(@Context sender: CommandSender, @Arg player: Optional<Player>) {
        val playerToReload = player.getOrNull() ?: sender as? Player
        if(playerToReload == null) {
            sender.sendMessage(Format.error("Only players can use this command if the player argument is not provided!"))
            return
        }

        sender.sendMessage(Format.info("Sent signal to remove resource packs..."))
        playerToReload.clearResourcePacks()

        val request = ResourcePackRequest.resourcePackRequest()
            .packs(PlayerController.resourcePackInfo)
            .prompt(Format.mm("The event requires a resource pack for certain UI elements to render correctly."))
            .required(true)
            .callback { uuid, status, audience ->
                if(status != ResourcePackStatus.SUCCESSFULLY_LOADED) {
                    if(!status.intermediate()) {
                        playerToReload.kick(Format.mm("<red>Resource pack load failed. Please rejoin or try again later.</red>"))
                    }
                    return@callback
                }

                sender.sendMessage(Format.success("Reloaded resource pack successfully!"))
            }
            .build()

        playerToReload.sendResourcePacks(request)
        sender.sendMessage(Format.info("Sent signal to add resource pack..."))
    }
}