package xyz.devcmb.tumblers.engine.map

import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection

data class LoadedMap(val id: String, val world: World, val data: ConfigurationSection)