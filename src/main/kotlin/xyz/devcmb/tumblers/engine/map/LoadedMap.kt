package xyz.devcmb.tumblers.engine.map

import org.bukkit.Location
import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.util.unpackCoordinates

class LoadedMap(
    val id: String,
    val world: World,
    val data: ConfigurationSection
)