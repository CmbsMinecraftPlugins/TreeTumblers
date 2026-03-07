package xyz.devcmb.tumblers.engine.cutscene

import org.bukkit.Location
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.engine.LoadedMap
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync

class CutsceneContext(val observers: Set<Player>, val map: LoadedMap) {
    suspend fun teleport(x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        observers.forEach {
            // TODO: Replace this with moving a set of pigs that the teams are riding on
            suspendSync {
                it.teleport(Location(map.world, x, y, z, pitch, yaw))
                it.isFlying = true
            }
        }
    }
}