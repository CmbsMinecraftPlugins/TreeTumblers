package xyz.devcmb.tumblers.engine.map

import org.bukkit.GameRules
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.MapSetupException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.WorldController
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
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
 * @property game The instance of the game which holds this map
 * @throws MapSetupException Throws if there is no data corresponding to the map
 */
class Map(
    val id: String,
) {
    lateinit var game: GameBase

    fun init(game: GameBase) {
        this.game = game
    }

    suspend fun load(index: Int): LoadedMap {
        val worldController = ControllerDelegate.getController("worldController") as WorldController

        val config = TreeTumblers.plugin.config
        val gameWorlds = config.getString("${game.configRoot}.worlds_folder")
            ?.replace("&", TreeTumblers.plugin.dataFolder.path.toString())
            ?: throw MapSetupException("Parent worlds folder could not be found")

        val worldName = config.getString("${game.configRoot}.maps.$id.world")
            ?: throw MapSetupException("Map world name could not be found")

        val world = worldController.loadTemplate(
            Path(WorldController.worldRoot, gameWorlds, worldName),
            "${game.id}_${id}-$index"
        )

        suspendSync {
            world.setGameRule(GameRules.SPAWN_MOBS, false)
            world.setGameRule(GameRules.ADVANCE_TIME, false)
            world.setGameRule(GameRules.ADVANCE_WEATHER, false)

            world.setGameRule(GameRules.FALL_DAMAGE, !game.flags.contains(Flag.DISABLE_FALL_DAMAGE))
            world.setGameRule(GameRules.PVP, !game.flags.contains(Flag.DISABLE_PVP))
            world.setGameRule(GameRules.LOCATOR_BAR, game.flags.contains(Flag.ENABLE_LOCATOR_BAR))
            world.setGameRule(
                GameRules.FIRE_SPREAD_RADIUS_AROUND_PLAYER,
                if(game.flags.contains(Flag.ENABLE_FIRE_SPREAD)) 128 else 0
            )
        }

        val dataPath = "${game.configRoot}.maps.$id.data"
        val data: ConfigurationSection =
            config.getConfigurationSection(dataPath)
                ?: YamlConfiguration()

        return LoadedMap(id, world, data)
    }
}