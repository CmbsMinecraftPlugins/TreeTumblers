package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.Format

/**
 * A single step of a [Cutscene]
 *
 * @param chatMessage The message to send all observers of the cutscene
 * @param run The function to run with a [LoadedMap] param and the [Cutscene] object's [CutsceneContext]
 */
class CutsceneStep(
    val chatMessage: Component?,
    val startingTeleport: String? = null,
    val run: suspend CutsceneContext.(map: LoadedMap) -> Unit
) {
    companion object {
        val GLHF = CutsceneStep(Format.mm("<b><green>Good Luck, Have Fun!</green></b>"), null) {}
    }
}