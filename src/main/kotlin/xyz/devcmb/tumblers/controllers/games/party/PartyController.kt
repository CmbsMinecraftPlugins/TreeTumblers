package xyz.devcmb.tumblers.controllers.games.party

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.party.games.individual.StandardSwordDuels
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.validateLocation
import java.io.File
import java.nio.file.Files
import java.nio.file.Path

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
    flags = setOf(Flag.DISABLE_FALL_DAMAGE, Flag.DISABLE_BLOCK_BREAKING),
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
    lateinit var pivot: Location
    var nextXPosition: Int = 0
    val activeIndividualGames: ArrayList<IndividualPartyGame> = ArrayList()

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        map = loadMap(maps.first(), 1)
        val pivotLocation = map.data.getList("game_pivot")
            ?.validateLocation(map.world)
            ?: throw GameControllerException("Game pivot location not provided in map data")

        pivot = pivotLocation
        nextXPosition = pivotLocation.x.toInt()
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
                it.enableBossBar("countdownBossbar")
                it.teleport(spawn)
            }
        }
    }

    override suspend fun gamePregame() {
        asyncCountdown(10, "party_pregame")
        delay(3000)
        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
            Format.mm("<white>First up</white>"),
            Format.mm("<yellow><b>Individual games</b></yellow>"),
            Title.Times.times(Tick.of(5), Tick.of(60), Tick.of(5))
        ))
        delay(4000)
        repeat(3) {
            val color = when(it) {
                0 -> NamedTextColor.GREEN
                1 -> NamedTextColor.YELLOW
                2 -> NamedTextColor.RED
                else -> NamedTextColor.WHITE
            }

            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(Title.title(
                Format.mm("Games begin in"),
                Format.mm("<b>> ${3 - it} <</b>").color(color),
                Title.Times.times(Tick.of(0), Tick.of(25), Tick.of(0))
            ))
            delay(1000)
        }
    }

    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        asyncCountdown(10 * 60)
        while(true) {
            // TODO: remove for obvious reasons
            runIndividualGame(Bukkit.getOnlinePlayers().first(), null, individualGames.random())
            delay(5000)
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(10000)
    }

    suspend fun runIndividualGame(player1: Player, player2: Player?, game: Class<out IndividualPartyGame>) {
        val gameClass = game
            .getDeclaredConstructor(Player::class.java, Player::class.java)
            .newInstance(player1, player2)
        activeIndividualGames.add(gameClass)

        val schematicFolder = File(partyGamesDirectory, gameClass.id)
        if(!schematicFolder.exists() || !schematicFolder.isDirectory) throw GameControllerException("Game ${gameClass.id} does not have any schematic files")

        val chosenSchematic = Files.list(Path.of(schematicFolder.path)).toList().random()
        val chosenFile = File(chosenSchematic.toString())
        val format = ClipboardFormats.findByFile(chosenFile)

        if(format == null) throw GameControllerException("Schematic $chosenSchematic for game ${gameClass.id} is not a valid schematic!")

        val clipboard: Clipboard
        format.getReader(chosenFile.inputStream()).use { reader ->
            clipboard = reader.read()
        }

        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(map.world))
            .fastMode(true)
            .build()

        val pos = BukkitAdapter.adapt(pivot).toBlockPoint().withX(nextXPosition)
        nextXPosition += clipboard.maximumPoint.x() + 3

        val operation = ClipboardHolder(clipboard)
            .createPaste(editSession)
            .to(pos)
            .ignoreAirBlocks(false)
            .build()

        Operations.complete(operation)
        editSession.flushQueue()
        editSession.close()
    }
}