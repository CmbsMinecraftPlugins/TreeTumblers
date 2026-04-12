package xyz.devcmb.tumblers.controllers.games.party

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.NamespacedKey
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.party.games.individual.StandardSwordDuels
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File

/*
 * Party is quick-time action game where teams and players face head to head in minigames
 * Whenever a game finishes, both teams are sent into a pool waiting for another new team to become available
 * Points are based on game wins. All games reward equal score and take about the same amount of time
 * For the first 5m of the game, all games are individual, so you get individual score which overall contributes to the team
 * For the last 5m of the game, all games are team-based, so you get team score which is evenly distributed to everyone on the team
 *
 * Misc stuff to keep in mind
 * [ ] All minigames should give team boots
 *
 * Individual game ideas:
 * [ ] Standard sword duels (stone sword)
 * [ ] Standard axe duels (stone axe)
 * [ ] Standard bow duels (crossbow duels, 3-shot kill)
 * [ ] Mace duels (wind charge, 1 hit kill, mace)
 * [ ] Sumo (Fist-fight, last man standing wins)
 * [ ] Quickdraw (single shot gun with instakill)
 * [ ] Pillars (navigate a short parkour course to reach the other side)
 * [ ] Pearl fight (knockback sticks and 2 pearls on a small map)
 * [ ] Horseback spear duels (1 hit kill, netherite spears)
 * [ ] Ice boat race (small track, first to complete wins)
 * [ ] Riptide trident race (fastest one to complete a short trident course wins)
 *
 * Team game ideas:
 * [ ] Standard sword duels
 * [ ] Standard axe duels
 * [ ] Standard bow duels
 */
@EventGame
class PartyController : GameBase(
    id = "party",
    name = "Party",
    votable = true,
    maps = setOf(
        Map("main")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/deathrun")))
            .append(Component.text(" Party"))
        ) {
            teleportConfig("cutscene.start")
            delay(5000)
        },
    ),
    flags = setOf(),
    scores = hashMapOf(),
    icon = Component.empty(),
    scoreboard = "partyScoreboard"
) {
    data class PartyGame(val id: String)
    data class PartyGameSchematic(val file: File)

    companion object {
        val individualGames: ArrayList<Class<out IndividualPartyGame>> = arrayListOf(
            StandardSwordDuels::class.java
        )
        val teamGames: ArrayList<Class<out TeamPartyGame>> = arrayListOf()

        val individualIds: List<String> = individualGames.map {
            it.getDeclaredConstructor().newInstance().id
        }

        val teamIds: List<String> = teamGames.map {
            it.getDeclaredConstructor().newInstance().id
        }

        @field:Configurable("templates.party_games")
        var partyGamesDirectory: String = "&/templates/party"
            get() {
                return field.replace("&", TreeTumblers.plugin.dataPath.toString())
            }
    }

    lateinit var map: LoadedMap

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        map = loadMap(maps.first(), 1)
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

        val spawn = map.data.getList("pregame_spawn")
            ?.validateLocation(map.world)
            ?: throw GameControllerException("Game world does does not contain a valid pregame spawn")

        suspendSync {
            gameParticipants.forEach {
                it.enableBossBar("countdown")
                it.teleport(spawn)
            }
        }
    }

    override suspend fun gamePregame() {
        countdown(10)

    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        delay(100000)
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(10000)
    }
}