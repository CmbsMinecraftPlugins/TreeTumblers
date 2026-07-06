package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.Tag
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.entity.Player
import org.bukkit.event.entity.EntityDamageEvent
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import java.util.UUID

object Format {
    val miniMessage: MiniMessage = MiniMessage.builder()
        .tags(TagResolver.builder()
            .resolver(StandardTags.defaults())
            .resolver(TagResolver.resolver("line") { args, _ ->
                var component = Component.empty()

                var length = try {
                    args.popOr { "Line argument must be a specified integer" }.value().toInt()
                } catch(_: NumberFormatException) {
                    DebugUtil.severe("Line argument was not a number!")
                    return@resolver Tag.inserting(Component.empty())
                }

                repeat(length * 2) {
                    component = component.append(
                        Component.text(" ")
                    )
                }

                component = component.decoration(TextDecoration.STRIKETHROUGH, true)

                Tag.inserting(component)
            })
            .resolver(TagResolver.resolver("team") { args, _ ->
                val team = args.popOr { "Team argument must be a specified team color!" }.value()
                val tumblingTeam = Team.entries.find { it.name.equals(team, ignoreCase = true) }
                if(tumblingTeam == null) {
                    throw IllegalStateException("Team argument must be a valid team color!")
                }

                var type = args.popOr { "Type argument must be a specified string of either \"name\" or \"icon\"" }.value()
                Tag.inserting(when(type) {
                    "name" -> tumblingTeam.formattedName
                    "icon" -> tumblingTeam.formattedIcon
                    else -> throw IllegalStateException("Type argument must be a specified string of either \"name\" or \"icon\"")
                })
            })
            .resolver(TagResolver.resolver("player") { args, _ ->
                val player = args.popOr { "Player argument must be a valid specified player UUID!" }.value()
                val tumblingPlayer = PlayerController.players.find { it.uuid == UUID.fromString(player) }
                if(tumblingPlayer == null) {
                    throw IllegalStateException("Player argument must be a valid specified player UUID!")
                }

                Tag.inserting(tumblingPlayer.formattedName)
            })
            .resolver(TagResolver.resolver("glyph") { args, _ ->
                val glyph = args.popOr { "Glyph must be a specified path!" }.value()
                Tag.inserting(Font.getGlyph(glyph))
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
                    team.formattedIcon
                )
                .append(Component.text(" "))
                .append(Component.text("Player", team.color))
        }

        val team = player.team
        return Component.empty()
            .append(
                team.formattedIcon
            )
            .append(Component.text(" "))
            .append(Component.text(player.name, team.color))
    }

    fun formatKillMessage(killer: Player?, killed: Player?, receiver: Player, score: Int)
        = formatKillMessage(killer?.tumblingPlayer, killed?.tumblingPlayer, receiver, score)

    fun formatKillMessage(killer: TumblingPlayer?, killed: TumblingPlayer?, receiver: Player, score: Int, scoreOverride: Int? = null): Component {
        require(killer != null || killed != null) { "Both killer and killed cannot be null" }

        val killerName =
            if(killer == null) Component.empty()
                .append(Team.SPECTATORS.formattedIcon)
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killer)

        val killedName =
            if(killed == null) Component.empty()
                .append(Team.SPECTATORS.formattedIcon)
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killed)

        var component = mm(
            "<gray>(<white><glyph:icon/skull></white>) <killed> was slain by <killer></gray>",
            Placeholder.component("killed", killedName),
            Placeholder.component("killer", killerName)
        )

        if((receiver == killer && score > 0) || scoreOverride != null) {
            component = component.append(Component.text(" [+${scoreOverride ?: score}]", NamedTextColor.GOLD))
        }

        return component
    }

    fun formatDeathMessage(killed: Player?, receiver: Player, grantScore: Boolean = false, score: Int = 0)
        = formatDeathMessage(killed?.tumblingPlayer, receiver, grantScore, score)

    fun formatDeathMessage(
        killed: TumblingPlayer?,
        receiver: Player,
        grantScore: Boolean = false,
        score: Int = 0,
        lastDamage: EntityDamageEvent.DamageCause? = null,
    ): Component {
        val killedName =
            if(killed == null) Component.empty()
                .append(Team.SPECTATORS.formattedIcon)
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killed)

        val message =
            if(lastDamage == null) MiniMessagePlaceholders.Game.CAUSELESS_DEATH_MESSAGES.random()
            else (MiniMessagePlaceholders.Game.CAUSED_DEATH_MESSAGES[lastDamage] ?: MiniMessagePlaceholders.Game.CAUSELESS_DEATH_MESSAGES.random())

        var result = mm(
            message,
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
            .append(level.icon())
            .append(Component.text(" $text", level.color))
    }

    fun log(text: Component, level: DebugUtil.DebugLogLevel) : Component {
        return Component.empty()
            .append(level.icon())
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
                    Placeholder.component("icon", GameController.activeGame?.data?.icon ?: Component.empty()),
                    Placeholder.component("message", message)
                )
            }
        };

        abstract fun formatMessage(message: Component): Component
    }
}