package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync

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
    val pigs: HashMap<Player, Entity> = HashMap()
    suspend fun run(observers: Set<Player>, map: LoadedMap, game: GameBase) {
        suspendSync {
            observers.forEach {
                observers.forEach { other ->
                    if(other == it) return@forEach
                    it.hidePlayer(TreeTumblers.plugin, other)
                }

                it.sendMessage(Component.empty()
                    .append(UserInterfaceUtility.constructLine(20, NamedTextColor.AQUA))
                    .append(Component.newline())
                    .append(chatMessage)
                    .append(Component.newline())
                    .append(UserInterfaceUtility.constructLine(20, NamedTextColor.AQUA))
                )
            }
        }

        val context = CutsceneContext(observers, map, this, game)
        context.init(map)
    }

    suspend fun cleanup(observers: Set<Player>) {
        suspendSync {
            observers.forEach {
                observers.forEach { other ->
                    if(other == it) return@forEach
                    it.showPlayer(TreeTumblers.plugin, other)
                }
            }
        }

        suspendSync {
            pigs.forEach {
                it.value.remove()
            }
        }
    }
}