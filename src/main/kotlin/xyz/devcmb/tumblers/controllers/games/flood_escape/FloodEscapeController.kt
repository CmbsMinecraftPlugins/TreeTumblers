package xyz.devcmb.tumblers.controllers.games.flood_escape

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
import xyz.devcmb.tumblers.engine.base.RoundedGame
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.validateLocation

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

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        repeat(rounds) {
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

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun preRound() {
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