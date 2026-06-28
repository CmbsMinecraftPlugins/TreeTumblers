package xyz.devcmb.tumblers.controllers.games.flood_escape

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
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.canReplaceActionBar
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import kotlin.math.roundToInt

@EventGame
class FloodEscapeController : RoundedGame(
    FloodEscapeData,
    configurable("games.flood_escape.rounds"),
    360
) {
    companion object {
        val font = NamespacedKey(TreeTumblers.NAMESPACE, "games/flood_escape")
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
            loadMap(data.maps.random(), it)
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
        Bukkit.broadcast(gameMessage(Format.mm("<red><player:${player.uuid}> was lost in the water!</red>")))

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
}