package xyz.devcmb.tumblers.engine

import org.bukkit.World
import org.bukkit.configuration.ConfigurationSection
import java.util.HashMap

data class LoadedMap(val id: String, val world: World, val data: ConfigurationSection)