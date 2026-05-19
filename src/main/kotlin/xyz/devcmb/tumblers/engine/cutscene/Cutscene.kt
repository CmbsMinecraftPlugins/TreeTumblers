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

/**
 * A cutscene comprised of [CutsceneStep]s
 *
 * @param steps The list of [CutsceneStep]s included in the cutscene
 * @param dontAutoCleanup If the cleanup method should automatically be called when all steps have finished
 */
class Cutscene(val steps: List<CutsceneStep>, val dontAutoCleanup: Boolean = false) {
    var currentStepIndex: Int = 0
    lateinit var context: CutsceneContext
    val pigs: HashMap<Player, Pig> = HashMap()
    val observers: MutableSet<Player> = HashSet()

    companion object {
        const val CUTSCENE_MESSAGE_FORMAT = "<white><green><line:38></green><br><br><message><br><br></white><green><line:38></green>"
    }

    /**
     * Runs the cutscene
     *
     * @param observers The players who are viewing the cutscene as it begins. See [addObserver] and [removeObserver] for adding or removing mid-cutscene
     * @param map The [LoadedMap] to perform the cutscene on
     * @param game The optional [GameBase] to associate the cutscene with
     */
    suspend fun run(observers: Set<Player>, map: LoadedMap, game: GameBase?) {
        this.observers.addAll(observers)

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
        suspendSync { cleanup() }
    }

    /** Cleans up the cutscene and calls [removeObserver] on all players */
    fun cleanup() {
        HandlerList.unregisterAll(context)
        pigs.forEach {
            it.value.remove()
        }
        pigs.clear()

        observers.toList().forEach(this::removeObserver)
        observers.clear()
    }

    /**
     * Runs the cutscene with a world instead of a [LoadedMap], essentially lying that its a game cutscene
     *
     * @param observers The players observing the cutscene
     * @param world The world where the cutscene takes place
     * @param config The configuration section to be given to the [LoadedMap] for [CutsceneContext] operations
     */
    suspend fun run(observers: Set<Player>, world: World, config: ConfigurationSection) =
        run(observers, LoadedMap(world.name, world, config), null)

    /**
     * Adds an observer to the cutscene
     *
     * @param player The player to add
     * @param midCutscene Whether or not the player is being added during the cutscene
     */
    fun addObserver(player: Player, midCutscene: Boolean) {
        observers.add(player)

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

    /**
     * Removes an observer from the cutscene
     *
     * @param player The player to remove
     */
    fun removeObserver(player: Player) {
        observers.remove(player)
        pigs[player]?.remove()
        pigs.remove(player)

        if(!player.isOnline) return

        player.showToAll()
        player.removePotionEffect(PotionEffectType.INVISIBILITY)
    }
}