package xyz.devcmb.tumblers.controllers.games.tower_ascent

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import com.sk89q.worldedit.world.block.BlockTypes
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentData
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentSpawn
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.MobSpawner
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.getPivot
import xyz.devcmb.tumblers.util.getPostPasteBounds
import xyz.devcmb.tumblers.util.getPostPasteLocation
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.titleCountdown
import xyz.devcmb.tumblers.util.toBlockVector3
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateElements
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import java.util.HashMap
import kotlin.io.path.Path
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@EventGame
class TowerAscentController : AbstractGame(TowerAscentData) {
    val map: LoadedMap
        get() = loadedMaps.first()

    val roomCount: Int = configurable("$configRoot.rooms")

    lateinit var generator: TowerGenerator
    lateinit var mobSpawner: MobSpawner

    val teamRoomSetIndexes: HashMap<Team, Int> = HashMap()
    val teamRooms: HashMap<Team, Int> = HashMap()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        val map = loadMap(data.maps.random(), 1)

        generator = TowerGenerator(this, map)
        generator.generateTowers()

        mobSpawner = MobSpawner(this, map)
        mobSpawner.loadMobSets()
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if(cycle != SpawnCycle.PREGAME) return

        suspendSync {
            Team.nonPlayingTeams.forEach {
                spawnPlayers(map, it.getOnlinePlayers(), TowerAscentSpawn.SET_1)
            }

            Team.playingTeams.forEachIndexed { index, team ->
                teamRoomSetIndexes[team] = index
                teamRooms[team] = 1

                generator.mapSpawns.getOrNull(index)?.let {
                    it.wallBounds.first.forEachRegion(it.wallBounds.second) { block ->
                        block.type = team.glass
                    }
                }

                spawnPlayers(map, team.getOnlinePlayers(), TowerAscentSpawn.valueOf("SET_${index + 1}"))
            }
        }
    }

    override suspend fun gamePregame() {
        timer(Timer(20.seconds) {
            id = "tower_ascent_game_start"
            title = "Game Start"
            joined = true

            timeExecution(10) {
                titleCountdown(
                    Audience.audience(gamePlayers.mapNotNull { it.bukkitPlayer }),
                    Format.mm("Game starts in"),
                    10
                )
            }
        })

        suspendSync {
            generator.mapSpawns.forEach {
                it.wallBounds.first.forEachRegion(it.wallBounds.second) { block ->
                    block.type = Material.AIR
                }
            }
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        timer(Timer(12.minutes) {
            id = "tower_ascent_game_on"
            title = "Game Over"
            joined = true
        })
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        val placements = getTeamPlacements()
        gameParticipants.mapNotNull { it.bukkitPlayer }.forEach { plr ->
            val teamPlacement = placements.find { it.first == plr.tumblingPlayer.team }!!.second

            val color = when(teamPlacement) {
                1 -> NamedTextColor.GOLD
                2 -> TextColor.fromHexString("#E0E0E0")
                3 -> TextColor.fromHexString("#CE8946")
                else -> NamedTextColor.AQUA
            }

            plr.showTitle(Title.title(
                Component.text("Game Over!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Format.mm("<white>Team <color:${color!!.asHexString()}>$teamPlacement${getOrdinalSuffix(teamPlacement)}</color> place!"),
                Title.Times.times(Tick.of(3), Tick.of(90), Tick.of(3))
            ))
            plr.sendMessage(gameMessage(Component.text("Game Over!")))
        }

        delay(5000)
        announceTeamScores()
        announceIndivScores()
        announceOverallTeamScores()
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