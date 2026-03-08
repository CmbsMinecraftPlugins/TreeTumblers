package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.event.HoverEvent
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player

object DebugUtil {
    val ansiColorMap: HashMap<NamedTextColor, String> = hashMapOf(
        NamedTextColor.RED to "\u001B[31m",
        NamedTextColor.GREEN to "\u001B[32m",
        NamedTextColor.YELLOW to "\u001B[33m",
        NamedTextColor.BLUE to "\u001B[34m",
        NamedTextColor.LIGHT_PURPLE to "\u001B[35m",
        NamedTextColor.AQUA to "\u001B[36m",
        NamedTextColor.WHITE to "\u001B[37m",
    )
    val reset = "\u001B[0m"

    val loggingSubscriptions: HashMap<Player, DebugLogLevel> = HashMap()

    fun subscribe(player: Player, level: DebugLogLevel) {
        loggingSubscriptions.put(player, level)
    }

    fun info(message: String) = log(message, DebugLogLevel.INFO)
    fun success(message: String) = log(message, DebugLogLevel.SUCCESS)
    fun warning(message: String) = log(message, DebugLogLevel.WARNING)
    fun severe(message: String) = log(message, DebugLogLevel.ERROR)

    fun log(message: String, level: DebugLogLevel) {
        assert(level != DebugLogLevel.NONE)

        Bukkit.getServer().consoleSender.sendMessage("${ansiColorMap[level.color]!!}[TreeTumblers ${level.name.lowercase()}]${reset} $message")

        val stackTrace = Thread.currentThread().stackTrace
        val caller = stackTrace[2]
        loggingSubscriptions.forEach { player, logLevel ->
            if(logLevel.level >= level.level) {
                player.sendMessage(
                    Component.text("[")
                        .append(Component.text(level.icon, NamedTextColor.WHITE)
                            .font(UserInterfaceUtility.WARNINGS))
                        .append(Component.text(" TreeTumblers] "))
                        .color(level.color)
                        .hoverEvent(HoverEvent.showText(
                            Component.text("${caller.className}:${caller.lineNumber}")
                        ))
                    .append(Component.text(message, NamedTextColor.WHITE))
                )
            }
        }
    }

    enum class DebugLogLevel(
        val level: Int,
        val color: NamedTextColor = NamedTextColor.WHITE,
        val icon: String = "",
    ) {
        NONE(0),
        ERROR(1, NamedTextColor.RED, "\uE004"),
        WARNING(2, NamedTextColor.YELLOW, "\uE005"),
        SUCCESS(3, NamedTextColor.GREEN, "\uE001"),
        INFO(4, NamedTextColor.AQUA, "\uE000"),
    }
}