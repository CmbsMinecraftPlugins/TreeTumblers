package xyz.devcmb.tumblers.engine

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.MapSetupException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.WorldController
import kotlin.io.path.Path

/**
 * A map that can be played during the event
 *
 * The config for all games is standard, but has an additional `data` field that contains stuff specific for that map and game.
 *
 * ```yml
 *  warfare:
 *      name: "Warfare"
 *      world: "crumble_warfare"
 *      data:
 *          spawns: []
 *          field: "value"
 * ```
 *
 * @param id The unique identifier for the map, as seen in the config
 * @property world The playing world, null if the map is not loaded
 * @property data The map data loaded from the config file
 * @throws MapSetupException Throws if there is no data corresponding to the map
 */
class Map(
    val id: String
) {
    lateinit var game: GameBase

    fun init(game: GameBase) {
        this.game = game
    }

    suspend fun load(index: Int): LoadedMap {
        val worldController = ControllerDelegate.getController("worldController") as WorldController

        val config = TreeTumblers.plugin.config
        val rootPath = config.getString("${game.configRoot}.worlds_folder")
            ?.replace("&", TreeTumblers.plugin.dataFolder.path.toString())
            ?: throw MapSetupException("Parent worlds folder could not be found")

        val worldName = config.getString("${game.configRoot}.maps.$id.world")
            ?: throw MapSetupException("Map world name could not be found")

        val world = worldController.loadTemplate(
            Path(rootPath, worldName),
            "${game.id}_${id}-$index"
        )

        val dataPath = "${game.configRoot}.maps.$id.data"
        val data: ConfigurationSection =
            config.getConfigurationSection("${game.configRoot}.maps.$id.data")
                ?: YamlConfiguration()

        return LoadedMap(id, world, data)
    }
}