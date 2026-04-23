package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Entity
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.hideToAll

/**
 * A single step of a cutscene.
 *
 * These are for explaining the game before it begins
 *
 * @param chatMessage The message to send all observers of the cutscene
 * @param init The function to run with a [LoadedMap] param and an attached [CutsceneContext]
 */
class CutsceneStep(
    val chatMessage: Component?,
    val startingTeleport: String? = null,
    val init: suspend CutsceneContext.(map: LoadedMap) -> Unit
) {
    val pigs: HashMap<Player, Entity> = HashMap()
    var context: CutsceneContext? = null
    suspend fun run(observers: Set<Player>, map: LoadedMap, game: GameBase?) {
        extraCutsceneWork(observers)

        context = CutsceneContext(observers, map, this, game)
        Bukkit.getPluginManager().registerEvents(context!!, TreeTumblers.plugin)
        startingTeleport?.let {
            context!!.teleportConfig(it)
        }
        context!!.init(map)
    }

    suspend fun run(observers: Set<Player>, world: World, config: ConfigurationSection) {
        extraCutsceneWork(observers)

        context = CutsceneContext(observers, world, config, this)
        Bukkit.getPluginManager().registerEvents(context!!, TreeTumblers.plugin)
        startingTeleport?.let {
            context!!.teleportConfig(it)
        }
        context!!.init(context!!.map)
    }

    suspend fun playerJoin(player: Player) {
        if(context == null) return
        startingTeleport?.let {
            context!!.teleportConfig(it, player)
            player.hideToAll()
        }
    }

    suspend fun extraCutsceneWork(observers: Set<Player>) {
        suspendSync {
            observers.forEach {
                it.hideToAll()

                if(chatMessage != null) {
                    it.sendMessage(Format.mm(
                        "<aqua><line:35><br><white><message></white><br><line:35></aqua>",
                        Placeholder.component("message", chatMessage)
                    ))
                }
            }
        }
    }

    suspend fun cleanup(observers: Set<Player>) {
        HandlerList.unregisterAll(context!!)

        suspendSync {
            observers.forEach {
                observers.forEach { other ->
                    if(other == it || !it.isOnline || !other.isOnline) return@forEach
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