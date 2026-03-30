package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.MiniMessage
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver
import net.kyori.adventure.text.minimessage.tag.standard.StandardTags
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility

object Format {
    val randomizedKillMessages: ArrayList<String> = arrayListOf(
        "{player} was killed by {killer}",
        "{player} got destroyed by {killer}",
        "{player} got smoked by {killer}",
        "{player} got obliterated by {killer}",
        "{player} got annihilated by {killer}",
        "{player} got wrecked by {killer}",
        "{player} was outplayed by {killer}",
        "{player} was outsmarted by {killer}",
        "{player} was outgunned by {killer}",
        "{player} was outmatched by {killer}",
        "{player} was outperformed by {killer}",
        "{player} was outdone by {killer}",
        "{player} was rejected by {killer}",
        "{player} was forgotten by {killer}",
        "{player} was disrespected by {killer}"
    )

    val randomizedDeathMessages: MutableList<String> = mutableListOf(
        "{player} tripped",
        "{player} didn't try hard enough",
        "{player} thought they could speed bridge",
        "{player} had a skill issue",
        "{player} became a pork chop",
        "{player} went kaboom",
        "{player} was caught playing fortnite",
        "{player} should get their eyes checked",
        "{player} should buy a better pc",
        "{player} got banned from discord",
        "{player} got rejected"
    )

    val miniMessage: MiniMessage = MiniMessage.builder()
        .tags(TagResolver.builder()
            .resolver(StandardTags.defaults())
            .build())
        .build()

    fun mm(text: String): Component {
        return miniMessage.deserialize(text)
    }

    fun mm(text: String, vararg placeholder: TagResolver): Component {
        return miniMessage.deserialize(text, *placeholder)
    }

    fun formatPlayerName(player: Player): Component {
        val tumblingPlayer = player.tumblingPlayer
        val team = tumblingPlayer.team
        return Component.empty()
            .append(
                Component.text(team.icon, NamedTextColor.WHITE)
                    .font(NamespacedKey("tumbling", "icons"))
            )
            .append(Component.text(" "))
            .append(Component.text(player.name, team.color))
    }

    fun formatKillMessage(killer: Player?, killed: Player?, receiver: Player, score: Int): Component {
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

        val template = randomizedKillMessages.random()

        // chatgpt regex magic (what is this)
        val parts = template.split(Regex("(\\{player}|\\{killer})"))
        val matches = Regex("(\\{player}|\\{killer})").findAll(template).map { it.value }.toList()

        var result = Component.empty()
        parts.indices.forEach {
            result = result.append(Component.text(parts[it]))

            if (it < matches.size) {
                result = result.append(
                    when (matches[it]) {
                        "{player}" -> killedName
                        "{killer}" -> killerName
                        else -> Component.empty()
                    }
                )
            }
        }

        if(receiver == killer) {
            result = result.append(Component.text(" [+$score]"))
        }
        result = result.color(NamedTextColor.GRAY)

        return result.color(NamedTextColor.GRAY)
    }

    fun formatDeathMessage(killed: Player?, receiver: Player, grantScore: Boolean = false, score: Int = 0): Component {
        val killedName =
            if(killed == null) Component.empty()
                .append(
                    Component.text(Team.SPECTATORS.icon, NamedTextColor.WHITE)
                        .font(NamespacedKey("tumbling", "icons"))
                )
                .append(Component.text(" "))
                .append(Component.text("Player", NamedTextColor.WHITE))
            else formatPlayerName(killed)

        val template = randomizedDeathMessages.random()

        // chatgpt regex magic (what is this)
        val parts = template.split(Regex("(\\{player}|\\{killer})"))
        val matches = Regex("(\\{player}|\\{killer})").findAll(template).map { it.value }.toList()

        var result = Component.empty()
        parts.indices.forEach {
            result = result.append(Component.text(parts[it]))

            if (it < matches.size) {
                result = result.append(
                    when (matches[it]) {
                        "{player}" -> killedName
                        else -> Component.empty()
                    }
                )
            }
        }

        if(grantScore) {
            result = result.append(Component.text(" [+$score]"))
        }
        result = result.color(NamedTextColor.GRAY)

        return result.color(NamedTextColor.GRAY)
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
}