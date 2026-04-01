package xyz.devcmb.tumblers.controllers.games.deathrun.traps

import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.block.data.BlockData
import org.bukkit.configuration.ConfigurationSection
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController
import xyz.devcmb.tumblers.controllers.games.deathrun.Trap
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.unpackCoordinates

class BeamRunTrap(
    override val deathrunController: DeathrunController,
    override val data: ConfigurationSection,
    override val from: Location,
    override val to: Location
) : Trap {
    override val name: Component = Format.mm("<bold><yellow>Beam Run Trap</yellow></bold>")
    override val id: String = "beam_run"
    override val itemKey: NamespacedKey = NamespacedKey("tumbling", "deathrun/beam_run_trap")
    override val cooldown: Int = 10

    override suspend fun activate() {
        data.getList("blocks")?.map {
            if(it !is List<*>) throw GameControllerException("Locations list does not contain location lists")
            it.map { int ->
                if(int !is Int) throw GameControllerException("Locations list does not contain exclusively integers")
                int.toDouble()
            }
        }?.forEach {
            TreeTumblers.pluginScope.async {
                val currentMap = deathrunController.currentMap
                val location = it.unpackCoordinates(currentMap.world)
                val sourceId = (0..10000000).random()
                val data: BlockData = location.block.blockData

                Bukkit.getOnlinePlayers().forEach { plr ->
                    plr.sendBlockDamage(location, 0.9f, sourceId)
                }
                delay(300)
                Bukkit.getOnlinePlayers().forEach { plr ->
                    plr.sendBlockDamage(location, 0f, sourceId)
                }

                val type = location.block.type
                currentMap.world.spawnParticle(
                    Particle.BLOCK,
                    location,
                    5,
                    0.0,
                    0.0,
                    0.0,
                    location.block.blockData
                )

                suspendSync {
                    location.block.type = Material.AIR
                }

                delay(2000)
                suspendSync {
                    location.block.type = type
                    location.block.blockData = data
                }
            }
        } ?: throw GameControllerException("Beam run locations table not provided")
    }
}