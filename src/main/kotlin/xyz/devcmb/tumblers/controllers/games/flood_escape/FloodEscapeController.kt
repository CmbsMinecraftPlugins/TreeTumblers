package xyz.devcmb.tumblers.controllers.games.flood_escape

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.math.BlockVector3
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockType
import com.sk89q.worldedit.world.block.BlockTypes
import kotlinx.coroutines.launch
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Gate
import org.bukkit.entity.BlockDisplay
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.Quaternionf
import org.joml.Vector3f
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.base.RoundedGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.canReplaceActionBar
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.getPostPasteBounds
import xyz.devcmb.tumblers.util.getPostPasteLocation
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.toBlockVector3
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import kotlin.io.path.Path
import kotlin.math.roundToInt

@EventGame
class FloodEscapeController : RoundedGame(
    FloodEscapeData,
    configurable("games.flood_escape.rounds"),
    360
) {
    companion object {
        val font = NamespacedKey(TreeTumblers.NAMESPACE, "games/flood_escape")
        val obstaclesDirectory: String = configurable("templates.flood_escape_obstacles")
            get() {
                return field.replace("&", TreeTumblers.plugin.dataPath.toString())
            }
    }

    val currentMap
        get() = loadedMaps.getOrNull(roundIndex) ?: loadedMaps.first()

    val alivePlayers: ArrayList<TumblingPlayer> = ArrayList()
    val playerPlacements: ArrayList<HashMap<TumblingPlayer, Int>> = ArrayList()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        repeat(rounds) {
            playerPlacements.add(hashMapOf())
            val map = loadMap(data.maps.random(), it)
            generateObstacles(it, map)
        }
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if(cycle != SpawnCycle.PRE_ROUND) return

        suspendSync {
            gamePlayers.filter { it.isOnline && !it.team.playingTeam }.forEach {
                makeSpectator(it.bukkitPlayer!!, sendActionBar = true, participating = false)
            }

            spawnPlayers(
                loadedMaps[roundIndex],
                gamePlayers.mapNotNull { it.bukkitPlayer }.toSet(),
                FloodEscapeSpawns.SPAWN
            )
        }
    }

    val obstacles: ArrayList<ArrayList<LoadedObstacle>> = ArrayList()
    val bridges: ArrayList<ArrayList<LoadedBridge>> = ArrayList()

    private fun generateObstacles(roundIndex: Int, map: LoadedMap) {
        val pivotStart = map.data.getList("pivot_start")
            ?.validateLocation(map.world)
            ?: throw GameControllerException("Map ${map.id} does not have a valid pivot start")

        val obstacles: ArrayList<LoadedObstacle> = ArrayList()
        val bridges: ArrayList<LoadedBridge> = ArrayList()

        var parent: Location = pivotStart
        repeat(30) {
            val (obstacle, obstacleEnd) = generateObstacle(parent, it, map)
            obstacles.add(obstacle)
            val (bridge, loc) = generateBridge(obstacleEnd, it, map)
            bridges.add(bridge)

            parent = loc
        }

        this.obstacles.add(obstacles)
        this.bridges.add(bridges)
    }

    fun generateObstacle(startLocation: Location, obstacleIndex: Int, map: LoadedMap): Pair<LoadedObstacle, Location> {
        val difficulty = Difficulty.entries.reversed().find { it.appearAfter >= obstacleIndex }
            ?: throw GameControllerException("No difficulty could be found for obstacle $obstacleIndex")

        val type = Type.entries.random()

        val mapObstacles = File(obstaclesDirectory, map.id)
        val obstaclePoolDir = File(Path(
            mapObstacles.path,
            difficulty.name.lowercase(),
            type.name.lowercase()
        ).toString())

        if(!obstaclePoolDir.exists() || !obstaclePoolDir.isDirectory)
            throw GameControllerException("Obstacle pool for ${map.id} (difficulty $difficulty and type $type) does not have a corresponding folder")

        val selectedObstacleFile = obstaclePoolDir.listFiles().random()
        val (clipboard, endPivotWorld) = loadSchematic(selectedObstacleFile, startLocation, map)

        return LoadedObstacle(
            obstacleIndex,
            difficulty,
            type,
            clipboard.getPostPasteBounds(startLocation)
        ) to endPivotWorld
    }

    fun generateBridge(startLocation: Location, obstacleIndex: Int, map: LoadedMap): Pair<LoadedBridge, Location> {
        val mapBridges = File(Path(obstaclesDirectory, map.id, "bridge").toString())
        if(!mapBridges.exists() || !mapBridges.isDirectory) throw GameControllerException("Map ${map.id} does not have a valid bridges folder")

        val selectedBridgeFile = mapBridges.listFiles().random()
        val (clipboard, endPivotWorld) = loadSchematic(selectedBridgeFile, startLocation, map)

        return LoadedBridge(
            obstacleIndex,
            clipboard.getPostPasteBounds(startLocation)
        ) to endPivotWorld
    }

    fun loadSchematic(file: File, location: Location, map: LoadedMap): Pair<Clipboard, Location> {
        val format = ClipboardFormats.findByFile(file)
            ?: throw GameControllerException("Selected obstacle ${file.name} is not a valid schematic file")

        val clipboard = format.getReader(file.inputStream()).use { reader ->
            reader.read()
        }

        clipboard.origin = getPivot(clipboard, BlockTypes.REDSTONE_BLOCK!!)
            ?: throw GameControllerException("A pivot line of 5 redstone blocks was not found in the schematic")

        val endPivot = getPivot(clipboard, BlockTypes.DIAMOND_BLOCK!!)
            ?: throw GameControllerException("A pivot line of 5 diamond blocks was not found in the schematic")

        clipboard.region.forEach { pos ->
            val block = clipboard.getBlock(pos)
            if (block.blockType == BlockTypes.NETHERITE_BLOCK || block.blockType == BlockTypes.REDSTONE_BLOCK || block.blockType == BlockTypes.DIAMOND_BLOCK) {
                clipboard.setBlock(pos.x(), pos.y(), pos.z(), BlockTypes.AIR!!.defaultState)
            }
        }

        val holder = ClipboardHolder(clipboard)
        val session = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(map.world))
            .fastMode(true)
            .build()

        val operation = holder
            .createPaste(session)
            .to(location.toBlockVector3())
            .ignoreAirBlocks(true)
            .build()

        Operations.complete(operation)
        session.flushQueue()

        return clipboard to clipboard.getPostPasteLocation(endPivot, location)
    }

    fun getPivot(clipboard: Clipboard, type: BlockType): BlockVector3? {
        clipboard.region.forEach { origin ->
            if (clipboard.getBlock(origin).blockType != type) return@forEach

            fun check(dx: Int, dz: Int): BlockVector3? {
                for (i in -2..2) {
                    val pos = BlockVector3.at(
                        origin.x() + dx * i,
                        origin.y(),
                        origin.z() + dz * i
                    )

                    if (!clipboard.region.contains(pos)) return null
                    if (clipboard.getBlock(pos).blockType != type) return null
                }
                return origin
            }

            val xCheck = check(1,0)
            val zCheck = check(0,1)

            if(xCheck != null || zCheck != null) return xCheck ?: zCheck
        }

        return null
    }

    data class LoadedObstacle(
        val index: Int,
        val difficulty: Difficulty,
        val type: Type,
        val bounds: Pair<Location, Location>
    )

    data class LoadedBridge(
        val index: Int,
        val bounds: Pair<Location, Location>
    )

    enum class Difficulty(val appearAfter: Int) {
        EASY(0),
        MEDIUM(10),
        HARD(20),
        EXTREME(30)
    }

    enum class Type {
        NORMAL,
        ELYTRA,
        TRIDENT
    }

    override suspend fun gamePregame() {
    }

    // Blocks/second
    val startingSpeed: Double = configurable("$configRoot.starting_speed")
    var waterSpeed: Double = startingSpeed

    var waterTask: BukkitRunnable? = null
    var waterKillTask: BukkitRunnable? = null

    var water: BlockDisplay? = null
    var currentWaterMovementDirection: MovementDirection? = null

    val playerDistances: HashMap<TumblingPlayer, Double> = HashMap()

    override suspend fun preRound() {
        playerDistances.clear()
        alivePlayers.addAll(gameParticipants)
        super.preRound()
    }

    override suspend fun startRound() {
        val gateStart: Location = currentMap.data.getList("gate_start")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate start not found")

        val gateEnd: Location = currentMap.data.getList("gate_end")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate end not found")

        suspendSync {
            gateStart.forEachRegion(gateEnd) {
                if(it.blockData !is Gate) return@forEachRegion
                it.blockData = (it.blockData as Gate).also { gate ->
                    gate.isOpen = true
                }
            }
        }

        alivePlayers.filter { !it.isOnline }.sortedBy { it.team.priority }.toList().forEach { eliminatePlayer(it) }

        currentTimer!!.timeExecution(340) {
            val world = currentMap.world
            val startingPosition = currentMap.data.getList("water.start_position")
                ?.validateLocation(world)
                ?: throw GameControllerException("Water start position not found")

            currentWaterMovementDirection = MovementDirection.entries
                .find { it.identifier == (currentMap.data.getString("water.movement_direction") ?: throw GameControllerException("Water movement direction not found")) }
                ?: throw GameControllerException("Water movement direction matching ${currentMap.data.getString("water.movement_direction")} could not be found")

            val leftRotation = currentMap.data.getList("water.left_rotation")
                ?.validateList<Number>()
                ?.map { it.toFloat() }
                ?: throw GameControllerException("Water left rotation was not found")

            val rightRotation = currentMap.data.getList("water.right_rotation")
                ?.validateList<Number>()
                ?.map { it.toFloat() }
                ?: throw GameControllerException("Water right rotation was not found")

            val scale = currentMap.data.getList("water.scale")
                ?.validateList<Number>()
                ?.map { it.toFloat() }
                ?: throw GameControllerException("Water scale rotation was not found")

            suspendSync {
                water = world.spawn(startingPosition, BlockDisplay::class.java) {
                    it.block = Material.BLUE_CONCRETE.createBlockData()
                    it.transformation = Transformation(
                        Vector3f(),
                        Quaternionf(leftRotation[0], leftRotation[1], leftRotation[2], leftRotation[3]),
                        Vector3f(scale[0], scale[1], scale[2]),
                        Quaternionf(rightRotation[0], rightRotation[1], rightRotation[2], rightRotation[3]),
                    )
                }

                waterTask = object : BukkitRunnable() {
                    override fun run() {
                        water!!.teleport(currentWaterMovementDirection!!.increase(water!!.location, (waterSpeed / 20).toFloat()))

                        if(!canReplaceActionBar()) return
                        alivePlayers.mapNotNull { it.bukkitPlayer }.forEach {
                            it.sendActionBar(Format.mm(
                                "<white><aqua>Water Distance:</aqua> ${currentWaterMovementDirection!!.axisDifference(water!!.location, it.location).roundToInt()}</white>"
                            ))
                        }
                    }
                }
                waterTask!!.runTaskTimer(TreeTumblers.plugin, 0, 1)

                waterKillTask = object : BukkitRunnable() {
                    override fun run() {
                        alivePlayers.toList().forEach {
                            if(!it.isOnline) return@forEach
                            if(currentWaterMovementDirection!!.axisDifference(water!!.location, it.bukkitPlayer!!.location) < 0) {
                                it.bukkitPlayer!!.damage(4.0)
                            }
                        }
                    }
                }
                waterKillTask!!.runTaskTimer(TreeTumblers.plugin, 0, 40)
            }
            Bukkit.broadcast(gameMessage(Format.mm("<aqua>The water has started moving!</aqua>")))
        }

        currentTimer!!.intervalExecution(50) {
            waterSpeed *= 1.75
            Bukkit.broadcast(gameMessage(Format.mm("<red>Water speed increased!</red>")))
        }
    }

    override suspend fun postRound() {
        waterTask?.cancel()
        waterKillTask?.cancel()
        alivePlayers.clear()

        super.postRound()
    }

    fun getWaterDistance(player: Player): Int {
        return currentWaterMovementDirection
            ?.axisDifference(water?.location ?: Location(currentMap.world, 0.0, 0.0, 0.0), player.location)
            ?.toInt()
            ?: 0
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        spawnPlayers(
            currentMap,
            setOf(player),
            FloodEscapeSpawns.SPAWN
        )

        if(!player.tumblingPlayer.team.playingTeam) return

        if(!preRound) {
            makeSpectator(player, false)
            player.sendMessage(Format.warning("You've joined while the round is active and have been placed into spectator. You will be put into the game next round."))
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        if(!player.tumblingPlayer.team.playingTeam) return

        TreeTumblers.pluginScope.launch {
            if(roundActive) {
                eliminatePlayer(player.tumblingPlayer)
            }
        }
    }

    suspend fun eliminatePlayer(player: TumblingPlayer) {
        playerPlacements[roundIndex][player] = alivePlayers.size
        alivePlayers.remove(player)

        gameParticipants.forEach {
            if(it in alivePlayers) grantScore(it, FloodEscapeScoreSource.OUTLAST_OPPONENT)
        }

        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            it.sendMessage(gameMessage(Format.mm("<red><player:${player.uuid}> was lost in the water!</red>" +
                if(it.tumblingPlayer in alivePlayers) " <gold>[+${getScoreSource(FloodEscapeScoreSource.OUTLAST_OPPONENT)}]</gold>" else ""
            )))
        }

        if(alivePlayers.size <= 1) {
            if(alivePlayers.size == 1) playerPlacements[roundIndex][alivePlayers.first()] = 1
            endRound()
        }
    }

    @EventHandler
    fun openGateEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        if(block.blockData is Gate) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun floodEscapePlayerDeathEvent(event: PlayerDeathEvent) {
        if(event.player.tumblingPlayer !in alivePlayers) return

        TreeTumblers.pluginScope.launch {
            eliminatePlayer(event.player.tumblingPlayer)
        }
    }

    enum class MovementDirection(
        val identifier: String,
        val increase: (location: Location, amount: Float) -> Location,
        val axisDifference: (origin: Location, target: Location) -> Double,
    ) {
        X_PLUS("x+", { location, amount ->
            val loc = location.clone()
            loc.x += amount
            loc
        }, { origin, target ->
            target.x - origin.x
        }),
        X_MINUS("x-", { location, amount ->
            val loc = location.clone()
            loc.x -= amount
            loc
        }, { origin, target ->
            origin.x - target.x
        }),
        Z_PLUS("z+", { location, amount ->
            val loc = location.clone()
            loc.z += amount
            loc
        }, { origin, target ->
            target.z - origin.z
        }),
        Z_MINUS("z-", { location, amount ->
            val loc = location.clone()
            loc.z -= amount
            loc
        }, { origin, target ->
            origin.z - target.z
        })
    }

    enum class FloodEscapeScoreSource(override val id: String) : ScoreSource {
        COMPLETE_OBSTACLE("flood_escape_complete_obstacle"),
        OUTLAST_OPPONENT("flood_escape_outlast_opponent")
    }
}