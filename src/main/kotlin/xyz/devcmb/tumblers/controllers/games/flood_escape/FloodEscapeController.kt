package xyz.devcmb.tumblers.controllers.games.flood_escape

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.block.data.type.Gate
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.subtitleCountdown
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.validateLocation

@EventGame
class FloodEscapeController : GameBase(
    id = "flood_escape",
    name = "Flood Escape",
    votable = true,
    maps = setOf(Map("sewer")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(font))
                .append(Component.text(" Flood Escape")),
            "cutscene.start"
        ) {
            delay(5000)
        }
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP,
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    tabLogo = Component.text("\uEA02").font(font)
        .shadowColor(ShadowColor.none()),
    scores = hashMapOf(),
    scoreboard = "floodEscapeScoreboard",
    spawns = FloodEscapeSpawns.entries
) {
    companion object {
        val font = NamespacedKey(TreeTumblers.NAMESPACE, "games/flood_escape")
    }

    val rounds: Int = configurable("$configRoot.rounds")
    var currentRound: Int = 0
    val roundIndex
        get() = currentRound - 1
    val currentMap
        get() = loadedMaps.getOrNull(roundIndex) ?: loadedMaps.first()

    val alivePlayers: ArrayList<TumblingPlayer> = ArrayList()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        repeat(rounds) {
            loadMap(maps.random(), it)
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

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    var roundEnded = false
    var roundActive = false
    override suspend fun gameOn() {
        repeat(rounds) {
            currentRound++
            preRound()
            suspendSync { startRound() }
            timer(Timer(360) {
                id = "flood_escape_round_active"
                title = "Round Over"
                onComplete { early ->
                    if(!early) roundEnded = true
                }
            })
            roundActive = true

            while(!roundEnded) {
                delay(500)
            }
            roundActive = false
        }
    }

    suspend fun preRound() {
        spawn(SpawnCycle.PRE_ROUND)
        timer(Timer(10) {
            id = "flood_escape_pre_round"
            title = "${if(currentRound == 1) "Game" else "Round"} Starts"
        })

        delay(2000)

        val title = Format.mm("<yellow><b>Round $currentRound</b></yellow>")
        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            it.showTitle(Title.title(
                title,
                Component.empty(),
                Title.Times.times(Tick.of(3), Tick.of(999), Tick.of(0))
            ))
        }

        delay(3000)

        subtitleCountdown(Audience.audience(gamePlayers.mapNotNull { it.bukkitPlayer }), title, 5)
        alivePlayers.addAll(gameParticipants)
    }

    fun startRound() {
        val gateStart: Location = currentMap.data.getList("gate_start")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate start not found")

        val gateEnd: Location = currentMap.data.getList("gate_end")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate end not found")

        gateStart.forEachRegion(gateEnd) {
            if(it.blockData !is Gate) return@forEachRegion
            it.blockData = (it.blockData as Gate).also { gate ->
                gate.isOpen = true
            }
        }
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

    @EventHandler
    fun openGateEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        if(block.blockData is Gate) {
            event.isCancelled = true
        }
    }
}