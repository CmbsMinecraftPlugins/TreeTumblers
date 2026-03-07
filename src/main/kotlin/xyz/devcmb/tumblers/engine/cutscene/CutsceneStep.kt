package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.engine.LoadedMap

class CutsceneStep(
    val chatMessage: Component,
    val init: suspend CutsceneContext.(map: LoadedMap) -> Unit
) {
    suspend fun run(observers: Set<Player>, map: LoadedMap) {
        observers.forEach {
            it.sendMessage(Component.empty()
                .append(Component.text("---------------------------------------", NamedTextColor.AQUA))
                .append(Component.newline())
                .append(chatMessage)
                .append(Component.newline())
                .append(Component.text("---------------------------------------", NamedTextColor.AQUA))
            )
        }

        val context = CutsceneContext(observers, map)
        context.init(map)
    }
}