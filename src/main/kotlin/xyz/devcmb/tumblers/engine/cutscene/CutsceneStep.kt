package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.ui.UserInterfaceUtility

/**
 * A single step of a cutscene.
 *
 * These are for explaining the game before it begins
 *
 * @param chatMessage The message to send all observers of the cutscene
 * @param init The function to run with a [LoadedMap] param and an attached [CutsceneContext]
 */
class CutsceneStep(
    val chatMessage: Component,
    val init: suspend CutsceneContext.(map: LoadedMap) -> Unit
) {
    suspend fun run(observers: Set<Player>, map: LoadedMap) {
        observers.forEach {
            it.sendMessage(Component.empty()
                .append(UserInterfaceUtility.constructLine(20, NamedTextColor.AQUA))
                .append(Component.newline())
                .append(chatMessage)
                .append(Component.newline())
                .append(UserInterfaceUtility.constructLine(20, NamedTextColor.AQUA))
            )
        }

        val context = CutsceneContext(observers, map)
        context.init(map)
    }
}