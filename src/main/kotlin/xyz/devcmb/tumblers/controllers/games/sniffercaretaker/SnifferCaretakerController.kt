package xyz.devcmb.tumblers.controllers.games.sniffercaretaker

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import org.bukkit.entity.EntityType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import xyz.devcmb.tumblers.util.validateCoordinates

@EventGame
class SnifferCaretakerController : GameBase(
    id = "snifferCaretaker",
    votable = true,
    maps = setOf(
        Map("facility")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/crumble")))
                .append(Component.text(" Sniffer Caretaker"))
        ) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
    ),
    flags = emptySet(),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "snifferCaretakerScoreboard"
) {
    val currentMap: LoadedMap
        get() {
            return loadedMaps[0]
        }
    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(maps.random(), 1)

        suspendSync {
            Team.entries.filter { it.playingTeam }.forEach {
                val snifferSpawn = map.data.getList("sniffer_spawns.${it.name.lowercase()}")?.validateCoordinates()
                    ?: throw GameControllerException("Sniffer spawns not found")

                val snifferLocation = snifferSpawn.unpackCoordinates(map.world)
                map.world.spawnEntity(snifferLocation, EntityType.SNIFFER)

                // Five Big Booms
            }
        }
    }

    override suspend fun gamePregame() {
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if (cycle != SpawnCycle.PRE_ROUND) return
        val map = loadedMaps[0]

        suspendSync {
            gamePlayers.forEach {
                it.spigot().respawn()
                val tumblingPlayer = it.tumblingPlayer

                val playerSpawn = currentMap.data.getList("spawns.${tumblingPlayer.team.name.lowercase()}")?.validateCoordinates()
                    ?: throw GameControllerException("Spawn not found")

                val playerLocation = playerSpawn.unpackCoordinates(map.world)

                it.teleport(playerLocation)
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        spawn(SpawnCycle.PRE_ROUND)
        gamePlayers.forEach {
            it.enableBossBar("countdownBossbar")
        }
        countdown(30)
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(1000)
    }

}