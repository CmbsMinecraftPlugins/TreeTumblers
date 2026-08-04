package xyz.devcmb.tumblers.commands.dev

import io.papermc.paper.command.brigadier.CommandSourceStack
import net.kyori.adventure.resource.ResourcePackRequest
import net.kyori.adventure.resource.ResourcePackStatus
import org.bukkit.entity.Player
import org.incendo.cloud.annotations.Command
import org.incendo.cloud.annotations.Permission
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable

@Suppress("unused")
@Permission("tumbling.dev")
class PackCommand {
    val packURL: String = configurable("pack.url")
    val packHash: String = configurable("pack.hash")

    @Command("pack reload [player]")
    fun reloadPacks(context: CommandSourceStack, player: Player?) {
        val sender = context.sender
        if(player == null && sender !is Player) {
            sender.sendMessage(Format.error("Only players can use this command if the player argument is not provided!"))
            return
        }

        val playerToReload = player ?: sender as Player

        sender.sendMessage(Format.info("Sent signal to remove resource packs..."))
        playerToReload.clearResourcePacks()

        val request = ResourcePackRequest.resourcePackRequest()
            .packs(PlayerController.resourcePackInfo)
            .prompt(Format.mm("The event requires a resource pack for certain UI elements to render correctly."))
            .required(true)
            .callback { _, status, _ ->
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