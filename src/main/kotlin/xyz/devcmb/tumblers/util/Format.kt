package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.GameController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility

object Format {
    val gameController by lazy {
        ControllerDelegate.getController<GameController>()
    }

    val miniMessage: MiniMessage = MiniMessage.builder()
        .tags(TagResolver.builder()
            .resolver(StandardTags.defaults())
            .resolver(TagResolver.resolver("line") { args, context ->
                var component = Component.empty()

                var length = try {
                    args.popOr { "Line argument must be a specified integer" }.value().toInt()
                } catch(e: NumberFormatException) {
                    DebugUtil.severe("Line argument was not a number!")
                    return@resolver Tag.inserting(Component.empty())
                }

                repeat(length) {
                    component = component.append(
                        Component.text("—")
                            .append(
                                Component.text("\uF000")
                                    .font(UserInterfaceUtility.SPACES)
                            )
                    )
                }

                Tag.inserting(component)
            })
            .build())
        .build()

    fun mm(text: String): Component {
        return miniMessage.deserialize(text)
    }

    fun mm(text: String, vararg placeholder: TagResolver): Component {
        return miniMessage.deserialize(text, *placeholder)
    }

    fun format(message: Component, format: MessageFormatter): Component {
        return format.formatMessage(message)
    }

    fun formatPlayerName(player: Player?) = formatPlayerName(player?.tumblingPlayer)

    fun formatPlayerName(player: TumblingPlayer?): Component {
        if(player == null) {
            val team = Team.SPECTATORS
            return Component.empty()
                .append(
                    Component.text(team.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text("Player", team.color))
        }

        val team = player.team
        return Component.empty()
            .append(
                Component.text(team.icon, NamedTextColor.WHITE)
                    .font(NamespacedKey("tumbling", "icons"))
            )
            .append(Component.text(" "))
            .append(Component.text(player.name, team.color))
    }

    fun formatKillMessage(killer: Player?, killed: Player?, receiver: Player, score: Int)
        = formatKillMessage(killer?.tumblingPlayer, killed?.tumblingPlayer, receiver, score)

    fun formatKillMessage(killer: TumblingPlayer?, killed: TumblingPlayer?, receiver: Player, score: Int): Component {
        require(killer != null || killed != null) { "Both killer and killed cannot be null" }

        val killerName =
            if(killer == null) Component.empty()
                .append(
                    Component.text(Team.SPECTATORS.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killer)

        val killedName =
            if(killed == null) Component.empty()
                .append(
                    Component.text(Team.SPECTATORS.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killed)

        var component = mm(
            "<killed> was slain by <killer>",
            Placeholder.component("killed", killedName),
            Placeholder.component("killer", killerName)
        )

        if(receiver == killer) {
            component = component.append(Component.text(" [+$score]", NamedTextColor.GOLD))
        }

        return component
    }

    fun formatDeathMessage(killed: Player?, receiver: Player, grantScore: Boolean = false, score: Int = 0)
        = formatDeathMessage(killed?.tumblingPlayer, receiver, grantScore, score)

    fun formatDeathMessage(killed: TumblingPlayer?, receiver: Player, grantScore: Boolean = false, score: Int = 0): Component {
        val killedName =
            if(killed == null) Component.empty()
                .append(
                    Component.text(Team.SPECTATORS.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killed)

        var result = mm(
            MiniMessagePlaceholders.Game.DEATH_MESSAGES.random(),
            Placeholder.component("player", killedName)
        )

        if(grantScore) {
            result = result.append(mm(" <gold>[+$score]</gold>"))
        }

        return result
    }

    fun success(text: String) = log(text, DebugUtil.DebugLogLevel.SUCCESS)
    fun warning(text: String) = log(text, DebugUtil.DebugLogLevel.WARNING)
    fun error(text: String): Component = log(text, DebugUtil.DebugLogLevel.ERROR)
    fun info(text: String) = log(text, DebugUtil.DebugLogLevel.INFO)

    fun success(text: Component) = log(text, DebugUtil.DebugLogLevel.SUCCESS)
    fun warning(text: Component) = log(text, DebugUtil.DebugLogLevel.WARNING)
    fun error(text: Component) = log(text, DebugUtil.DebugLogLevel.ERROR)
    fun info(text: Component) = log(text, DebugUtil.DebugLogLevel.INFO)

    fun log(text: String, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(Component.text(level.icon, NamedTextColor.WHITE).font(UserInterfaceUtility.WARNINGS))
            .append(Component.text(" $text", level.color))
    }

    fun log(text: Component, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(Component.text(level.icon, NamedTextColor.WHITE).font(UserInterfaceUtility.WARNINGS))
            .append(Component.text(" "))
            .append(text).color(level.color)
    }

    enum class MessageFormatter(val id: String) {
        DEFAULT("default") {
            override fun formatMessage(message: Component): Component {
                return message
            }
        },

        GAME_MESSAGE("game") {
            override fun formatMessage(message: Component): Component {
                return mm(
                    "<yellow>(<white><icon></white><yellow>)</yellow> <white><message></white>",
                    Placeholder.component("icon", gameController.activeGame?.icon ?: Component.empty()),
                    Placeholder.component("message", message)
                )
            }
        };

        abstract fun formatMessage(message: Component): Component
    }
}