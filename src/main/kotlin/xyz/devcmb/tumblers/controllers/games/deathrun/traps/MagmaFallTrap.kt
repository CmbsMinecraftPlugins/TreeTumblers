package xyz.devcmb.tumblers.controllers.games.deathrun.traps

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.controllers.games.deathrun.Trap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.unpackCoordinates

class MagmaFallTrap(
    override val deathrunController: DeathrunController,
    override val data: ConfigurationSection,
    override val from: Location,
    override val to: Location,
) : Trap {
    override val name: Component = Format.mm("<bold><yellow>Magma Fall Trap</yellow></bold>")
    override val id: String = "magma_fall"
    override val itemKey: NamespacedKey = NamespacedKey("tumbling", "deathrun/magma_fall_trap")
    override val cooldown: Int = 10

    override suspend fun activate() {
        val replaceStart: Location = data.getList("replace_start")?.map {
            if(it !is Int) throw DeathrunController.DeathrunTrapException("Location list does not contain exclusively integers")
            it.toDouble()
        }?.unpackCoordinates(deathrunController.currentMap.world)
            ?: throw DeathrunController.DeathrunTrapException("Magma fall replace start field not specified")

        val replaceEnd: Location = data.getList("replace_end")?.map {
            if(it !is Int) throw DeathrunController.DeathrunTrapException("Location list does not contain exclusively integers")
            it.toDouble()
        }?.unpackCoordinates(deathrunController.currentMap.world)
            ?: throw DeathrunController.DeathrunTrapException("Magma fall replace end field not specified")

        val replaceBlockType: Material = data.getString("replace_type")?.let { type -> Material.entries.first { it.name == type } }
            ?: throw DeathrunController.DeathrunTrapException("Replace type not specified")

        val oldBlocks: HashMap<Location, Material> = HashMap()
        suspendSync {
            replaceStart.forEachRegion(replaceEnd) {
                if(it.type == replaceBlockType) {
                    oldBlocks.put(it.location, it.type)
                    it.type = Material.RED_CONCRETE
                }
            }
        }

        delay(600)

        suspendSync {
            replaceStart.forEachRegion(replaceEnd) {
                if(it.type == Material.RED_CONCRETE) {
                    it.type = Material.AIR
                }
            }
        }

        delay(4000)

        suspendSync {
            oldBlocks.forEach {
                it.key.block.type = it.value
            }
        }
    }
}