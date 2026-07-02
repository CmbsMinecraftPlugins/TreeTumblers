package xyz.devcmb.tumblers.engine.cutscene

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleExitEvent
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.validateLocation

/**
 * A context object for a running [Cutscene] instance
 *
 * @param cutscene The cutscene object
 * @param observers A set of [Player] instances that are viewing the cutscene at the time of its creation. See [Cutscene.addObserver] and [Cutscene.removeObserver] for modifying this while a cutscene is running.
 * @param map The [LoadedMap] where the cutscene is happening. Does not have to be associated with a game
 * @param game The optional [AbstractGame] instance that this is attached to. Optional and is only required to pass on to the game object to the [CutsceneStep] lambda
 */
class CutsceneContext(
    val cutscene: Cutscene,
    val observers: Set<Player>,
    val map: LoadedMap,
    val game: AbstractGame?,
): Listener {
    /**
     * Teleports a specified [player] or all observers to a certain location based on the result of [getLocation] with the specified [path]
     *
     * @param path The path of the location array
     * @param player An optional player to specifically apply the teleportation to
     */
    suspend fun teleportConfig(path: String, player: Player? = null) {
        val location = getLocation(path)
        val players = if(player != null) listOf(player) else observers

        suspendSync {
            players.forEach {
                if(!it.isOnline) return@forEach

                it.tp(location)
                createPassengerPig(it, location)
            }
        }
    }

    /**
     * Get a location from the loaded map's configuration section
     *
     * @param path The path in the [LoadedMap]s [org.bukkit.configuration.ConfigurationSection]
     * @return The [Location] corresponding to the [path]
     * @throws GameControllerException If the teleport path failed to resolve to a valid location list
     */
    fun getLocation(path: String): Location {
        val config = map.data.getList(path) ?: throw GameControllerException("Teleport config path did not resolve to a valid list")
        val location: Location =
            config.validateLocation(map.world) ?: throw GameControllerException("Teleport config did not resolve to a valid location list")

        return location
    }

    /**
     * Creates a pig for the specified [player] to ride during a cutscene
     *
     * @param player The player to create the pig for
     * @param location The location to spawn the pig at
     */
    private fun createPassengerPig(player: Player, location: Location) {
        location.chunk.load()
        if(cutscene.pigs.containsKey(player)) {
            val pig = cutscene.pigs[player]!!
            pig.teleport(location)
            player.setRotation(location.yaw, location.pitch)
            return
        }

        val pig = map.world.spawnEntity(location, EntityType.PIG) as Pig
        pig.setAI(false)
        pig.isInvulnerable = true
        pig.isSilent = true
        pig.isInvisible = true
        pig.addPassenger(player)

        cutscene.pigs[player] = pig
    }

    @EventHandler
    fun playerDismountEvent(event: VehicleExitEvent) {
        val player = event.exited
        if(player !is Player) return

        event.isCancelled = true
    }
}