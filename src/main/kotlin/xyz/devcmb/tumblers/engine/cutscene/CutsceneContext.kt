package xyz.devcmb.tumblers.engine.cutscene

import org.bukkit.Location
import org.bukkit.entity.EntityType
import org.bukkit.entity.Pig
import org.bukkit.entity.Player
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

class CutsceneContext(val observers: Set<Player>, val map: LoadedMap, val step: CutsceneStep) {
    suspend fun teleport(x: Double, y: Double, z: Double, pitch: Float, yaw: Float) {
        suspendSync {
            observers.forEach {
                val location = Location(map.world, x, y, z, pitch, yaw)
                createPassengerPig(it, location)
                it.setRotation(location.yaw, location.pitch)
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
                val location = location.unpackCoordinates(map.world)
                it.teleport(location)

                createPassengerPig(it, location)
            }
        }
    }

    private fun createPassengerPig(player: Player, location: Location) {
        val pig = map.world.spawnEntity(location, EntityType.PIG) as Pig
        pig.setAI(false)
        pig.addPassenger(player)

        pig.addPotionEffect(PotionEffect(
            PotionEffectType.INVISIBILITY,
            Integer.MAX_VALUE,
            255,
            true,
            false
        ))

        step.pigs.put(player, pig)
    }
}