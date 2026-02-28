package xyz.devcmb.tumblers.util

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers


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

        loggingSubscriptions.forEach { player, logLevel ->
            if(logLevel.level >= level.level) {
                player.sendMessage(
                    Component.text("[TreeTumblers ${level.name.lowercase()}] ", level.color)
                        .append(Component.text(message, NamedTextColor.WHITE))
                )
            }
        }
    }

    enum class DebugLogLevel(
        val level: Int,
        val color: NamedTextColor = NamedTextColor.WHITE,
        val logFunction: (log: String) -> Unit = TreeTumblers.pluginLogger::info
    ) {
        NONE(0),
        ERROR(1, NamedTextColor.RED, TreeTumblers.pluginLogger::severe),
        WARNING(2, NamedTextColor.YELLOW, TreeTumblers.pluginLogger::warning),
        SUCCESS(3, NamedTextColor.GREEN),
        INFO(4, NamedTextColor.AQUA),
    }
}