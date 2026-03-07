package xyz.devcmb.tumblers.engine.cutscene

import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

class CutsceneContext(val observers: Set<Player>, val map: LoadedMap) {
    suspend fun teleport(x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        suspendSync {
            observers.forEach {
                // TODO: Replace this with moving a set of pigs that the teams are riding on
                it.teleport(Location(map.world, x, y, z, pitch, yaw))
                it.isFlying = true
            }
        }
    }

    suspend fun teleportConfig(path: String) {
        val config = map.data.getList(path) ?: throw GameControllerException("Teleport config path did not resolve to a valid list")
        val location: List<Double> = config.map {
            if(it !is Double) throw GameControllerException("Teleport list does not contain exclusively doubles")
            it
        }

        if(location.size < 3) {
            throw GameControllerException("Teleport list does not have enough elements")
        }

        suspendSync {
            observers.forEach {
                it.teleport(location.unpackCoordinates(map.world))
                it.isFlying = true
            }
        }
    }
}