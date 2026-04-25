package xyz.devcmb.tumblers.engine.cutscene

import net.kyori.adventure.text.Component
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.vehicle.VehicleExitEvent
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

class CutsceneContext(
    val observers: Set<Player>,
    val map: LoadedMap,
    val step: CutsceneStep,
    val game: GameBase?,
): Listener {
    constructor(observers: Set<Player>, world: World, config: ConfigurationSection, step: CutsceneStep):
        this(observers, LoadedMap(world.name, world, config), step, null)

    suspend fun teleport(x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        suspendSync {
            observers.forEach {
                if(!it.isOnline) return@forEach

                val location = Location(map.world, x, y, z, pitch, yaw)
                createPassengerPig(it, location)
                it.setRotation(location.yaw, location.pitch)
            }
        }
    }

    suspend fun teleportConfig(path: String, player: Player? = null) {
        val location = getLocation(path)
        val players = if(player != null) listOf(player) else observers

        suspendSync {
            players.forEach {
                if(!it.isOnline) return@forEach

                it.teleport(location)
                createPassengerPig(it, location)
            }
        }
    }

    fun title(title: Component? = null, subtitle: Component? = null, times: Title.Times? = null) {
        observers.forEach {
            if(!it.isOnline) return@forEach

            it.showTitle(Title.title(
                title ?: Component.empty(),
                subtitle ?: Component.empty(),
                times
            ))
        }
    }

    fun getLocation(path: String): Location {
        val config = map.data.getList(path) ?: throw GameControllerException("Teleport config path did not resolve to a valid list")
        val location: List<Double> = config.map {
            if(it !is Double) throw GameControllerException("Teleport list does not contain exclusively doubles")
            it
        }

        return location.unpackCoordinates(map.world)
    }

    private fun createPassengerPig(player: Player, location: Location) {
        val pig = map.world.spawnEntity(location, EntityType.PIG) as Pig
        pig.setAI(false)
        pig.isInvulnerable = true
        pig.isSilent = true
        pig.isInvisible = true
        pig.addPassenger(player)

        step.pigs.put(player, pig)
    }

    @EventHandler
    fun playerDismountEvent(event: VehicleExitEvent) {
        val player = event.exited
        if(player !is Player) return

        event.isCancelled = true
    }
}