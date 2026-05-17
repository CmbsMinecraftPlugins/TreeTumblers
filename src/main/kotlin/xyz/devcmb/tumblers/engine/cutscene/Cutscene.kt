package xyz.devcmb.tumblers.engine.cutscene

import kotlinx.coroutines.launch
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.event.HandlerList
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.showToAll

class Cutscene(val steps: List<CutsceneStep>, val dontAutoCleanup: Boolean = false) {
    var currentStepIndex: Int = 0
    lateinit var context: CutsceneContext
    val pigs: HashMap<Player, Pig> = HashMap()

    companion object {
        const val CUTSCENE_MESSAGE_FORMAT = "<white><green><line:38></green><br><br><message><br><br></white><green><line:38></green>"
    }

    suspend fun run(observers: Set<Player>, map: LoadedMap, game: GameBase?) {
        context = CutsceneContext(this, observers, map, game)
        Bukkit.getPluginManager().registerEvents(context, TreeTumblers.plugin)

        suspendSync { observers.forEach { addObserver(it, false) } }

        steps.forEachIndexed { index, step ->
            if(index != 0) currentStepIndex++

            step.startingTeleport?.let { path ->
                context.teleportConfig(path)
            }

            step.chatMessage?.let { message ->
                observers.forEach {
                    it.sendMessage(Format.mm(
                        CUTSCENE_MESSAGE_FORMAT,
                        Placeholder.component("message", message)
                    ))
                }
            }

            step.run(context, map)
        }

        if(dontAutoCleanup) return
        suspendSync { cleanup(observers) }
    }

    fun cleanup(observers: Set<Player>) {
        HandlerList.unregisterAll(context)
        pigs.forEach {
            it.value.remove()
        }
        pigs.clear()

        observers.forEach(this::removeObserver)
    }

    suspend fun run(observers: Set<Player>, world: World, config: ConfigurationSection) =
        run(observers, LoadedMap(world.name, world, config), null)

    fun addObserver(player: Player, midCutscene: Boolean) {
        player.hideToAll()
        player.addPotionEffect(PotionEffect(
            PotionEffectType.INVISIBILITY,
            PotionEffect.INFINITE_DURATION,
            1,
            true,
            false,
            false
        ))
        player.inventory.clear()

        TreeTumblers.pluginScope.launch {
            if(midCutscene) {
                val step = steps[currentStepIndex]
                step.chatMessage?.let {
                    player.sendMessage(Format.mm(
                        CUTSCENE_MESSAGE_FORMAT,
                        Placeholder.component("message", it)
                    ))
                }
                step.startingTeleport?.let { context.teleportConfig(it, player) }
            }
        }
    }

    fun removeObserver(player: Player) {
        player.showToAll()
        player.removePotionEffect(PotionEffectType.INVISIBILITY)
        pigs[player]?.remove()
        pigs.remove(player)
    }
}