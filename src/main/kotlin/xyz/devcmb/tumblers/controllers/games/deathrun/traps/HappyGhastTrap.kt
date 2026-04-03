package xyz.devcmb.tumblers.controllers.games.deathrun.traps

import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Fireball
import org.bukkit.util.Vector
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.controllers.games.deathrun.Trap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

class HappyGhastTrap(
    override val deathrunController: DeathrunController,
    override val data: ConfigurationSection,
    override val from: Location,
    override val to: Location
) : Trap {
    override val name: Component = Format.mm("<bold><yellow>Happy Ghast Trap</yellow></bold>")
    override val id: String = "happy_ghast"
    override val itemKey: NamespacedKey = NamespacedKey("tumbling", "deathrun/happy_ghast_trap")
    override val cooldown: Int = 15

    override suspend fun activate() {
        suspendSync {
            data.getList("ghasts")?.map {
                if(it !is List<*>) throw DeathrunController.DeathrunTrapException("Locations list does not contain location lists")
                it.map { int ->
                    if(int !is Double) throw DeathrunController.DeathrunTrapException("Locations list does not contain exclusively doubles")
                    int
                }
            }?.forEach {
                val location = it.unpackCoordinates(deathrunController.currentMap.world)
                location.world.spawn(
                    location.clone().add(0.0,12.0,0.0),
                    Fireball::class.java
                ) { fireball ->
                    fireball.velocity = Vector(0.0, -0.1, 0.0)
                }
            } ?: throw DeathrunController.DeathrunTrapException("Ghast location list not provided")
        }
    }
}