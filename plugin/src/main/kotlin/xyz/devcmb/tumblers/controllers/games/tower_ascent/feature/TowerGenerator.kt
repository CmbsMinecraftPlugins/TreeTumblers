package xyz.devcmb.tumblers.controllers.games.tower_ascent.feature

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Husk
import org.bukkit.entity.Mob
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Stray
import org.bukkit.entity.Zombie
import org.bukkit.event.HandlerList
import org.bukkit.inventory.ItemStack
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
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import xyz.devcmb.tumblers.util.withY
import java.io.File
import java.util.HashMap
import kotlin.collections.take
import kotlin.collections.takeLast
import kotlin.io.path.Path

class TowerGenerator(
    private val controller: TowerAscentController,
    private val map: LoadedMap
) {
    val mapSpawns: ArrayList<MapSpawn> = ArrayList()

    val loadouts: ArrayList<MobLoadout> = ArrayList()
    val spawnGroups: ArrayList<SpawnGroup> = ArrayList()
    val towerHandlers: ArrayList<TowerHandler> = ArrayList()

    val roomCount: Int = configurable("${controller.configRoot}.rooms")

    val templatesDirectory: String = configurable("templates.tower_ascent_templates")
        get() {
            return field.replace("&", TreeTumblers.plugin.dataPath.toString())
        }

    suspend fun generateTowers() {
        loadMobSets()

        val mapTemplates = File(templatesDirectory, map.id)

        val mapEndingElevator = loadSchematic(File(mapTemplates, "elevator_end.schem"))
        val mapStartingElevator = loadSchematic(File(mapTemplates, "elevator_start.schem"))
        val mapEndRoom = loadSchematic(File(mapTemplates, "ending.schem"))

        mapStartingElevator.origin = mapStartingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("Starting elevator for map ${map.id} does not have a diamond block origin line")

        mapEndingElevator.origin = mapEndingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("Ending elevator for map ${map.id} does not have a diamond block origin line")

        mapEndRoom.origin = mapEndRoom.getPivot(BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("Map ending room for map ${map.id} does not have a diamond block origin line")

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

            val endingRoomElevatorOperation = ClipboardHolder(mapStartingElevator)
                .createPaste(editSession)
                .to(startPos.toBlockVector3())
                .ignoreAirBlocks(true)
                .build()

            val pivot = mapStartingElevator.getPivot(BlockTypes.DIAMOND_BLOCK!!)!!
            val startingElevatorBounds = mapStartingElevator.getPostPasteBounds(startPos)
            startPos = mapStartingElevator.getPostPasteLocation(
                pivot,
                startPos
            )

            val endingRoomOperation = ClipboardHolder(mapEndRoom)
                .createPaste(editSession)
                .to(startPos.toBlockVector3())
                .ignoreAirBlocks(true)
                .build()

            Operations.complete(endingRoomElevatorOperation)
            Operations.complete(endingRoomOperation)
            editSession.flushQueue()

            val roomBounds = mapEndRoom.getPostPasteBounds(startPos)
            val endingBlocks: ArrayList<org.bukkit.util.Vector> = ArrayList()
            roomBounds.first.forEachRegion(roomBounds.second) {
                if(it.type == Material.WHITE_CONCRETE || it.type == Material.BLACK_CONCRETE) {
                    repeat(5) { index ->
                        endingBlocks.add(it.location.clone().withY(it.location.y + index).toBlockLocation().toVector())
                    }
                }
            }

            val endingRoom = LoadedEndingRoom(
                endingBlocks,
                startingElevatorBounds
            )

            val handler = TowerHandler(controller, map, loadouts, spawnGroups, loadedRooms, endingRoom)
            Bukkit.getPluginManager().registerEvents(handler, TreeTumblers.plugin)
            towerHandlers.add(handler)
        }

        editSession.close()
    }

    private fun getRooms(): List<RoomDefinition> {
        val mapTemplates = File(templatesDirectory, map.id)
        return map.data.getList("rooms")?.mapIndexed { index, room ->
            if(
                room !is HashMap<*, *>
                || !room.validateElements(hashMapOf(
                    "id" to { it is String },
                    "pivot_replacement_material" to { it is String && Material.entries.any { entry -> entry.name.equals(it, true) } },
                    "mob_sets" to { it is List<*> && it.all { setEntry ->
                        setEntry is List<*>
                        && setEntry.all { groupEntry ->
                            groupEntry is HashMap<*,*> && groupEntry["group"] is String && groupEntry["amount"] is Int
                        }
                    } }
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
                (room["mob_sets"] as List<*>).map {
                    val list = it as List<*>
                    list.map { listEntry ->
                        val map = listEntry as HashMap<*, *>
                        MobSet(map["group"] as String, map["amount"] as Int)
                    }
                },
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

    private fun loadMobSets() {
        val mapLoadouts = map.data.getList("loadouts")?.mapIndexed { index, loadout ->
            if(
                loadout !is HashMap<*, *>
                || !loadout.validateElements(hashMapOf(
                    "id" to { it is String },
                    "armor" to {
                        it is List<*>
                                && it.validateList<String>() != null
                                && it.all { entry ->
                            MobArmorItem.entries.any { armorEntry -> armorEntry.id.equals((entry as String), true) }
                        }
                    },
                    "weapon" to { it is String && MobWeaponItem.entries.any { entry -> entry.id.equals(it, true) } }
                ))
            ) throw GameControllerException("Loadout $index is not properly formatted")

            MobLoadout(
                loadout["id"] as String,
                (loadout["armor"] as List<*>).map {
                    MobArmorItem.entries.find { entry -> entry.id.equals(it as String, true) }!!
                },
                MobWeaponItem.entries.find { entry -> entry.id.equals(loadout["weapon"] as String, true) }!!
            )
        } ?: throw GameControllerException("Map loadouts for ${map.id} were not provided")

        val mapSpawnGroups = map.data.getList("spawn_groups")?.mapIndexed { index, group ->
            if(
                group !is HashMap<*, *>
                || !group.validateElements(hashMapOf(
                    "id" to { it is String },
                    "mob" to { it is String && SpawnableMob.entries.any { entry -> entry.id.equals(it, true)} },
                    "loadout" to { it is String && mapLoadouts.any { entry -> entry.id.equals(it, true) } }
                ))
            ) throw GameControllerException("Spawn group $index is not properly formatted")

            SpawnGroup(
                group["id"] as String,
                SpawnableMob.entries.find { it.id.equals(group["mob"] as String, true) }!!,
                group["loadout"] as String,
            )
        } ?: throw GameControllerException("Spawn groups for map ${map.id} were not provided")

        loadouts.addAll(mapLoadouts)
        spawnGroups.addAll(mapSpawnGroups)

        DebugUtil.info("Loaded the following loadouts: $mapLoadouts")
        DebugUtil.info("Loaded the following spawn groups: $mapSpawnGroups")
    }

    fun cleanup() {
        towerHandlers.forEach {
            HandlerList.unregisterAll(it)
        }
    }

    enum class Axis(val xIncrease: Int, val zIncrease: Int) {
        X(1, 0),
        Z(0, 1)
    }

    data class RoomDefinition(
        val id: String,
        val pivotReplacementMaterial: Material,
        val mobSets: List<List<MobSet>>,

        val clipboard: Clipboard
    )

    data class MobSet(val group: String, val amount: Int)

    data class LoadedRoom(
        val room: RoomDefinition,
        val roomBounds: Pair<Location, Location>,
        val startingElevatorBounds: Pair<Location, Location>?,
        val endingElevatorBounds: Pair<Location, Location>,
    )

    data class LoadedEndingRoom(
        val finish: List<org.bukkit.util.Vector>,
        val startingElevatorBounds: Pair<Location, Location>
    )

    data class MapSpawn(
        val pivot: Location,
        val wallBounds: Pair<Location, Location>,
    )

    data class SpawnGroup(
        val id: String,
        val mob: SpawnableMob,
        /** Relational to a [MobLoadout.id] */
        val loadout: String
    )

    data class MobLoadout(
        val id: String,
        val armor: List<MobArmorItem>,
        val weapon: MobWeaponItem
    )

    enum class SpawnableMob(val id: String, val entity: Class<out Mob>) {
        SKELETON("skeleton", Skeleton::class.java),
        STRAY("stray", Stray::class.java),
        ZOMBIE("zombie", Zombie::class.java),
        HUSK("husk", Husk::class.java)
    }

    enum class MobWeaponItem(val id: String, val item: ItemStack) {
        BASE_BOW("base_bow", ItemStack(Material.BOW)),
        STONE_SWORD("stone_sword", ItemStack(Material.STONE_SWORD)),
        IRON_SWORD("iron_sword", ItemStack(Material.IRON_SWORD)),
    }

    enum class MobArmorItem(val id: String, val item: ItemStack) {
        BASE_IRON_CHESTPLATE("base_iron_chestplate", ItemStack(Material.IRON_CHESTPLATE)),
        BASE_GOLD_HELMET("base_gold_helmet", ItemStack(Material.GOLDEN_HELMET)),
        BASE_LEATHER_CHESTPLATE("base_leather_chestplate", ItemStack(Material.LEATHER_CHESTPLATE)),
        BASE_IRON_BOOTS("base_iron_boots", ItemStack(Material.IRON_BOOTS)),
    }
}