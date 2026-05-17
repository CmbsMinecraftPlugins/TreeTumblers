package xyz.devcmb.tumblers.engine.map

import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import xyz.devcmb.tumblers.ControllerRegistry
import xyz.devcmb.tumblers.controllers.server.WorldController

/**
 * A loaded version of a [Map]
 *
 * @param id The unique identifier for the map
 * @param world The loaded [World] of the map
 * @param data The [ConfigurationSection] corresponding to the map
 */
class LoadedMap(
    val id: String,
    val world: World,
    val data: ConfigurationSection
) {
    val worldController: WorldController by ControllerRegistry.controller()

    /** Cleans up the loaded map */
    suspend fun cleanup() {
        worldController.cleanupWorld(world)
    }
}