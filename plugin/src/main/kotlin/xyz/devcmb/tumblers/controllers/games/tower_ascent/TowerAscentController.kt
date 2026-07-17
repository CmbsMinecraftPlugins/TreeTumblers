package xyz.devcmb.tumblers.controllers.games.tower_ascent

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.math.transform.AffineTransform
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Material
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.getPivot
import xyz.devcmb.tumblers.util.toBlockVector3
import xyz.devcmb.tumblers.util.validateElements
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import java.util.HashMap
import kotlin.io.path.Path

@EventGame
class TowerAscentController : AbstractGame(TowerAscentData) {
    val map: LoadedMap
        get() = loadedMaps.first()

    val roomCount: Int = configurable("$configRoot.rooms")

    companion object {
        val templatesDirectory: String = configurable("templates.tower_ascent_templates")
            get() {
                return field.replace("&", TreeTumblers.plugin.dataPath.toString())
            }
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(data.maps.random(), 1)
        val mapTemplates = File(templatesDirectory, map.id)

        val mapRooms = map.data.getList("rooms")?.mapIndexed { index, room ->
            if(
                room !is HashMap<*, *>
                || !room.validateElements(hashMapOf(
                    "id" to { it is String },
                    "pivot_replacement_material" to { it is String && Material.entries.any { entry -> entry.name.equals(it, true) } }
                ))
            ) throw GameControllerException("Room definition $index is not formatted properly")

            val id = room["id"] as String
            val schematic = File(Path(mapTemplates.path, "rooms", "$id.schem").toString())
            if(!schematic.exists() || !schematic.isFile) throw GameControllerException("Schematic $id was not found in the templates directory for ${map.id}")

            val format = ClipboardFormats.findByFile(schematic)
                ?: throw GameControllerException("Schematic file for room $id is not a valid schematic")

            val clipboard: Clipboard = format.getReader(schematic.inputStream()).use { reader ->
                reader.read()
            }

            val startPivot = clipboard
                .getPivot(BlockTypes.DIAMOND_BLOCK!!)
                ?: throw GameControllerException("Room $id does not contain a start pivot of 5 diamond blocks")

            val endPivot = clipboard
                .getPivot(BlockTypes.REDSTONE_BLOCK!!)
                ?: throw GameControllerException("Room $id does not contain an end pivot of 5 redstone blocks")

            RoomDefinition(
                id,
                Material.valueOf((room["pivot_replacement_material"] as String).uppercase()),
                clipboard,
                startPivot,
                endPivot
            )
        } ?: throw GameControllerException("Map data does not contain room data")

        val mapPlacementAxis = map.data.getString("placement_axis")
            ?.uppercase()
            ?: throw GameControllerException("Map placement axis was not specified in the map data")

        val placementAxis = Axis.valueOf(mapPlacementAxis)
        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(map.world))
            .fastMode(true)
            .build()

        val pivots = map.data.getList("pivots")
            ?.validateList<List<*>>()
            ?.map {
                it.validateLocation(map.world) ?: throw GameControllerException("Map room pivot is not a valid location")
            }
            ?: throw GameControllerException("Map room pivots were not provided")

        val rooms = (0..<roomCount).map { mapRooms.random() }
        pivots.forEach { pivot ->
            var startPos = pivot
            rooms.forEachIndexed { index, definition ->
                val room = mapRooms.random()
                val holder = ClipboardHolder(room.clipboard)

                if(index % 2 == 1) {
                    holder.transform = AffineTransform().rotateY(180.0)
                }

                val operation = holder
                    .createPaste(editSession)
                    .to(startPos.toBlockVector3())
                    .build()

                // TODO: Complete the operation and spawn the elevator
            }
        }
    }

    data class RoomDefinition(
        val id: String,
        val pivotReplacementMaterial: Material,
        val clipboard: Clipboard,
        val startPivot: BlockVector3,
        val endPivot: BlockVector3,
    )

    enum class Axis(val xIncrease: Int, val yIncrease: Int) {
        X(1, 0),
        Z(0, 1)
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        TODO("Not yet implemented")
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        TODO("Not yet implemented")
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        TODO("Not yet implemented")
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        TODO("Not yet implemented")
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        TODO("Not yet implemented")
    }
}