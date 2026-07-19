package xyz.devcmb.tumblers.controllers.games.tower_ascent.feature

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Location
import org.bukkit.Material
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentController
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.getPivot
import xyz.devcmb.tumblers.util.getPostPasteBounds
import xyz.devcmb.tumblers.util.getPostPasteLocation
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.toBlockVector3
import xyz.devcmb.tumblers.util.validateElements
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import java.util.HashMap
import kotlin.collections.take
import kotlin.collections.takeLast
import kotlin.io.path.Path

class TowerGenerator(
    controller: TowerAscentController,
    private val map: LoadedMap
) {
    val loadedRooms: ArrayList<ArrayList<LoadedRoom>> = ArrayList()
    val mapSpawns: ArrayList<MapSpawn> = ArrayList()
    val roomCount: Int = configurable("${controller.configRoot}.rooms")

    val templatesDirectory: String = configurable("templates.tower_ascent_templates")
        get() {
            return field.replace("&", TreeTumblers.plugin.dataPath.toString())
        }

    suspend fun generateTowers() {
        val mapTemplates = File(templatesDirectory, map.id)

        val mapEndingElevator = loadSchematic(File(mapTemplates, "elevator_end.schem"))
        val mapStartingElevator = loadSchematic(File(mapTemplates, "elevator_start.schem"))

        mapStartingElevator.origin = mapStartingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("Starting elevator for map ${map.id} does not have a diamond block origin line")

        mapEndingElevator.origin = mapEndingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("Ending elevator for map ${map.id} does not have a diamond block origin line")

        val mapRooms = getRooms()
        val spawns = getSpawns()

        val mapPlacementAxis = map.data.getString("placement_axis")
            ?.uppercase()
            ?: throw GameControllerException("Map placement axis was not specified in the map data")

        val placementAxis = Axis.valueOf(mapPlacementAxis)
        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(map.world))
            .fastMode(true)
            .build()

        val rooms = (0..<roomCount).map { mapRooms.random() }
        val loadedRooms: ArrayList<LoadedRoom> = ArrayList()
        spawns.forEach { spawn ->
            mapSpawns.add(spawn)

            var startPos = spawn.pivot
            rooms.forEachIndexed { index, room ->
                var startingElevatorBounds: Pair<Location, Location>? = null
                if(index != 0) {
                    val startingElevatorHolder = ClipboardHolder(mapStartingElevator)

                    val startingElevatorOperation = startingElevatorHolder
                        .createPaste(editSession)
                        .to(startPos.toBlockVector3())
                        .ignoreAirBlocks(true)
                        .build()
                    Operations.complete(startingElevatorOperation)

                    val pivot = mapStartingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)!!
                    startPos = mapStartingElevator.getPostPasteLocation(
                        pivot,
                        startPos
                    )
                    startingElevatorBounds = mapStartingElevator.getPostPasteBounds(startPos)
                }

                val roomOperation = ClipboardHolder(room.clipboard)
                    .createPaste(editSession)
                    .to(startPos.toBlockVector3())
                    .ignoreAirBlocks(true)
                    .build()
                DebugUtil.info("Loading room at $startPos")

                val roomEndPivot = room.clipboard.getPivot(BlockTypes.REDSTONE_BLOCK!!)!!
                val endPosWorld = room.clipboard.getPostPasteLocation(roomEndPivot, startPos)

                val elevatorOperation = ClipboardHolder(mapEndingElevator)
                    .createPaste(editSession)
                    .to(endPosWorld.toBlockVector3())
                    .ignoreAirBlocks(true)
                    .build()
                DebugUtil.info("Loading elevator at $endPosWorld")

                Operations.complete(roomOperation)
                Operations.complete(elevatorOperation)
                editSession.flushQueue()

                val roomBounds = room.clipboard.getPostPasteBounds(startPos)
                suspendSync {
                    roomBounds.first.forEachRegion(roomBounds.second) {
                        if(it.type == Material.REDSTONE_BLOCK || it.type == Material.DIAMOND_BLOCK)
                            it.type = room.pivotReplacementMaterial
                    }
                }

                startPos = startPos.add(
                    60.0 * placementAxis.xIncrease,
                    0.0,
                    60.0 * placementAxis.zIncrease
                )

                val endingElevatorBounds: Pair<Location, Location> = mapEndingElevator
                    .getPostPasteBounds(endPosWorld)

                loadedRooms.add(LoadedRoom(
                    room,
                    roomBounds,
                    startingElevatorBounds,
                    endingElevatorBounds
                ))
            }
        }

        this.loadedRooms.add(loadedRooms)
        editSession.close()
    }

    private fun getRooms(): List<RoomDefinition> {
        val mapTemplates = File(templatesDirectory, map.id)
        return map.data.getList("rooms")?.mapIndexed { index, room ->
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

            val startPivot = clipboard.getPivot(BlockTypes.DIAMOND_BLOCK!!)
                ?: throw GameControllerException("Room $id does not contain a start pivot of 5 diamond blocks")

            clipboard.getPivot(BlockTypes.REDSTONE_BLOCK!!)
                ?: throw GameControllerException("Room $id does not contain an end pivot of 5 redstone blocks")

            clipboard.origin = startPivot

            val pivotReplacementMaterial = Material.valueOf((room["pivot_replacement_material"] as String).uppercase())
            RoomDefinition(
                id,
                pivotReplacementMaterial,
                clipboard,
            )
        } ?: throw GameControllerException("Map data does not contain room data")
    }

    private fun getSpawns(): List<MapSpawn> {
        return map.data.getList("spawns")?.mapIndexed { index, spawn ->
            if(
                spawn !is HashMap<*, *>
                || !spawn.validateElements(hashMapOf(
                    "pivot" to { it is List<*> && it.validateLocation(map.world) != null },
                    "wall" to {
                        it is List<*>
                                && it.take(3).validateLocation(map.world) != null
                                && it.takeLast(3).validateLocation(map.world) != null
                    }
                ))
            ) throw GameControllerException("Spawn $index is not formatted properly")

            val wall = spawn["wall"] as List<*>
            MapSpawn(
                (spawn["pivot"] as List<*>).validateLocation(map.world)!!,
                Pair(
                    wall.take(3).validateLocation(map.world)!!,
                    wall.takeLast(3).validateLocation(map.world)!!
                )
            )
        } ?: throw GameControllerException("Spawns list not provided for map ${map.id}")
    }

    private fun loadSchematic(file: File): Clipboard {
        return ClipboardFormats.findByFile(file)?.getReader(file.inputStream())?.use { reader ->
            reader.read()
        } ?: throw GameControllerException("Failed to load ${file.name} to a clipboard")
    }

    enum class Axis(val xIncrease: Int, val zIncrease: Int) {
        X(1, 0),
        Z(0, 1)
    }

    data class RoomDefinition(
        val id: String,
        val pivotReplacementMaterial: Material,
        val clipboard: Clipboard
    )

    data class LoadedRoom(
        val room: RoomDefinition,
        val roomBounds: Pair<Location, Location>,
        val startingElevatorBounds: Pair<Location, Location>?,
        val endingElevatorBounds: Pair<Location, Location>,
    )

    data class MapSpawn(
        val pivot: Location,
        val wallBounds: Pair<Location, Location>,
    )
}