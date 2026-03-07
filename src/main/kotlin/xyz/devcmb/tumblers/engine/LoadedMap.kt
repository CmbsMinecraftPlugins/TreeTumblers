package xyz.devcmb.tumblers.engine

import org.bukkit.World
import java.util.HashMap

data class LoadedMap(val id: String, val world: World, val data: HashMap<String, out Any>)