package xyz.devcmb.tumblers.engine.map

import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection

/**
 * A map which has been loaded into multiple bukkit worlds
 * @param id The unique identifier for the map
 * @param world The loaded [World] of the map
 * @param data The [ConfigurationSection] corresponding to the map
 */
class LoadedMap(
    val id: String,
    val world: World,
    val data: ConfigurationSection
)