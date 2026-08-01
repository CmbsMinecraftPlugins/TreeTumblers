package xyz.devcmb.tumblers.controllers.player

import io.papermc.paper.event.player.AsyncChatEvent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer

@Controller
object ChatController : IController {
    var isChatMuted: Boolean = false
    val channels: HashMap<Player, ChatChannel> = HashMap()

    override fun init() {
    }

    @EventHandler
    fun playerMessageEvent(event: AsyncChatEvent) {
        if (isChatMuted) {
            event.player.sendMessage(Format.error("The chat is currently muted!"))
            event.isCancelled = true
            return
        }

        val channel = channels[event.player] ?: ChatChannel.LOCAL

        event.viewers().removeIf { viewer ->
            viewer is Player && !channel.canSee(event.player, viewer)
        }

        event.renderer { source, _, message, _ ->
            channel.format(source, message)
        }
    }

    fun muteChat() {
        PlayerController.isChatMuted = true
        Bukkit.broadcast(Format.info("The chat has been muted!"))
    }

    fun unmuteChat() {
        PlayerController.isChatMuted = false
        Bukkit.broadcast(Format.info("The chat has been unmuted!"))
    }

    enum class ChatChannel(val channelName: String, val color: TextColor) {
        LOCAL("Local", NamedTextColor.WHITE) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return true
            }

            override fun canSend(player: Player): Boolean {
                return true
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}><sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        },
        TEAM("Team", TextColor.fromHexString("#34d031")!!) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return sender?.tumblingPlayer?.team == receiver.tumblingPlayer.team
            }

            override fun canSend(player: Player): Boolean {
                return player.tumblingPlayer.team.playingTeam
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}>[Team] <sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        },
        STAFF("Staff", TextColor.fromHexString("#2aceff")!!) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return receiver.hasPermission("tumbling.dev") || receiver.hasPermission("tumbling.organizer")
            }

            override fun canSend(player: Player): Boolean {
                return canSee(null, player)
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}>[Staff] <sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        };

        abstract fun canSee(sender: Player?, receiver: Player): Boolean
        abstract fun canSend(player: Player): Boolean
        abstract fun format(sender: Player, message: Component): Component
    }
}