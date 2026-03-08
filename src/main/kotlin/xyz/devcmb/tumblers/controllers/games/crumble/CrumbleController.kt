package xyz.devcmb.tumblers.controllers.games.crumble

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    votable = true,
    maps = setOf(
        Map("warfare")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.text("Welcome to Crumble", NamedTextColor.YELLOW)) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Component.text("Cutscene step #2", NamedTextColor.GRAY)) { map ->
            teleport(0.0,128.0,0.0,0f,0f)
            delay(2000)
        }
    )
) {
    val rounds = run {
        Team.entries.filter { it.playingTeam }.size - 1
    }
    val currentRound = 1
    val matchups: ArrayList<MutableList<Pair<Team, Team>>> = ArrayList()

    override suspend fun gameLoad() {
        // ChatGPT code because idfk how to do any of this
        val teams = Team.entries.filter { it.playingTeam }.toMutableList()
        repeat(rounds) {
            val roundMatches = mutableListOf<Pair<Team, Team>>()

            for (i in 0 until teams.size / 2) {
                val a = teams[i]
                val b = teams[teams.lastIndex - i]
                roundMatches.add(a to b)
            }

            matchups.add(roundMatches)

            val last = teams.removeLast()
            teams.add(1, last)
        }

        for(i in 1..rounds) {
            val map = maps.random()
            loadMap(map, i)
        }
    }

    override suspend fun spawn(cycle: SpawnCycle) {
        when(cycle) {
            SpawnCycle.PREGAME -> {
                val currentMap = loadedMaps.getOrNull(0)
                if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

                val pregameSpawn = currentMap.data.getList("spawns.pregame")
                    ?: throw GameControllerException("Pregame spawn not specified for ${currentMap.id}")

                val location: List<Double> = pregameSpawn.map {
                    if(it !is Double) throw GameControllerException("Teleport list does not contain exclusively doubles")
                    it
                }

                suspendSync {
                    gamePlayers.forEach {
                        it.teleport(location.unpackCoordinates(currentMap.world))
                        DebugUtil.info("Spawned player ${it.name} at $location")
                    }
                }
            }
            SpawnCycle.PRE_ROUND -> {
                val currentMap = loadedMaps.getOrNull(currentRound - 1)
                if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

                val currentMatchups = matchups[currentRound - 1]
                val spawnSetKeys = mutableListOf(
                    "spawns.ingame.arena1"
                )

                currentMatchups.forEachIndexed { index, matchup ->
                    val spawns: List<List<List<Double>>> = currentMap.data
                        .getList(spawnSetKeys.getOrNull(index) ?: spawnSetKeys.first())
                        ?.map { l1 ->
                            if(l1 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                            return@map l1.map { l2 ->
                                if(l2 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                                return@map l2.map {
                                    if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
                                    it
                                }
                            }
                        } ?: throw GameControllerException("Spawn set not found")

                    var firstOccupiedSpawns = 0
                    var secondOccupiedSpawns = 0

                    gamePlayers.forEach {
                        val tumblingPlayer = it.tumblingPlayer ?: return@forEach

                        when(tumblingPlayer.team) {
                            matchup.first -> {
                                val firstSpawnSet = spawns[0]
                                val playerSpawn = firstSpawnSet[firstOccupiedSpawns]
                                val location = playerSpawn.unpackCoordinates(currentMap.world)

                                suspendSync {
                                    it.teleport(location)
                                }
                                DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                firstOccupiedSpawns++
                            }
                            matchup.second -> {
                                val secondSpawnSet = spawns[1]
                                val playerSpawn = secondSpawnSet[secondOccupiedSpawns]
                                val location = playerSpawn.unpackCoordinates(currentMap.world)

                                suspendSync {
                                    it.teleport(location)
                                }
                                DebugUtil.info("Spawned ${it.name} at $location")

                                secondOccupiedSpawns++
                            }
                            else -> return@forEach
                        }
                    }
                }
            }
        }
    }

    override suspend fun gameOn() {
    }

    override suspend fun pregame() {
        super.pregame()
    }
}