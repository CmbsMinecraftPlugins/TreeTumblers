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
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.party.games.shared.StandardSwordDuels
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.giveKit
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
    data class PartyGameIdentifier(val id: String)
    data class PartyGameSchematic(val file: File)

    companion object {
        val games: ArrayList<Class<out PartyGame>> = arrayListOf(
            StandardSwordDuels::class.java
        )

        val individualGames: List<Class<out PartyGame>>
            get() {
                return games.filter { it.getDeclaredConstructor().newInstance().individual }
            }

        val teamGames: List<Class<out PartyGame>>
            get() {
                return games.filter { it.getDeclaredConstructor().newInstance().team }
            }

        val gameIds: List<String> = games.map {
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
    val activeGames: ArrayList<PartyGame> = ArrayList()
    var currentGameType = PartyGameType.INDIVIDUAL

    val waitingPlayers: MutableSet<Player> = HashSet()
    val frozenPlayers: MutableSet<Player> = HashSet()

    lateinit var teamGamesTimer: Timer

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

//        teamGamesTimer = Timer("party_team_games_switchover_timer", 5 * 60) {
//
//        }

//        while(true) {
//            runGame(PartyMatchup.IndividualMatchup(this, Bukkit.getOnlinePlayers().first(), null), individualGames.random())
//            delay(20000)
//        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        delay(10000)
    }

    suspend fun runGame(matchup: PartyMatchup, game: Class<out PartyGame>) {
        matchup.announceLoading()

        val gameClass = game
            .getDeclaredConstructor(PartyGameType::class.java, PartyMatchup::class.java)
            .newInstance(currentGameType, matchup)
        activeGames.add(gameClass)

        if(
            (currentGameType == PartyGameType.INDIVIDUAL && !gameClass.individual)
            || (currentGameType == PartyGameType.TEAM && !gameClass.team)
        ) throw GameControllerException("Attempted to start a game mismatched with the current game type. Expected $currentGameType")

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

        DebugUtil.success("Loaded party arena $chosenSchematic successfully")

        val firstSideSpawns: ArrayList<Location> = ArrayList()
        val secondSideSpawns: ArrayList<Location> = ArrayList()

        clipboard.region.forEach {
            val worldPos = pos.add(it.subtract(clipboard.origin))
            val block = map.world.getBlockAt(worldPos.x(), worldPos.y(), worldPos.z())

            if(block.type == Material.WHITE_WOOL) {
                firstSideSpawns.add(block.location.add(0.5,1.0,0.5))
            } else if (block.type == Material.BLACK_WOOL) {
                secondSideSpawns.add(block.location.add(0.5,1.0,0.5))
            }
        }

        if(firstSideSpawns.isEmpty() || secondSideSpawns.isEmpty())
            throw GameControllerException("One or both sets of spawns are not marked through either black or white wool.")

        suspendSync {
            matchup.spawn(firstSideSpawns, secondSideSpawns)
        }
        matchup.concludeLoading()
        matchup.kitPlayers(gameClass.kit)
        gameClass.postSpawn()
        matchup.announceMatchup()
    }

    enum class PartyGameType {
        INDIVIDUAL,
        TEAM
    }

    sealed interface PartyMatchup {
        fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>)
        fun kitPlayers(kit: Kit.KitDefinition)
        fun announceLoading()
        fun concludeLoading()
        suspend fun announceMatchup()

        class IndividualMatchup(val partyController: PartyController?, val player1: Player?, val player2: Player?) : PartyMatchup {
            override fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>) {
                player1!!.teleport(spawns1.first())
                player1.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 255))
                player2?.teleport(spawns2.first())
                player2?.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 255))

                partyController!!.frozenPlayers.add(player1)
                if(player2 != null) partyController.frozenPlayers.add(player2)
            }

            override fun kitPlayers(kit: Kit.KitDefinition) {
                player1!!.giveKit(kit)
                player2?.giveKit(kit)
            }

            override fun announceLoading() {
                val title = Title.title(
                    Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                    Component.text("Loading...", NamedTextColor.AQUA),
                    Title.Times.times(Tick.of(0), Tick.of(9999999), Tick.of(0))
                )

                player1!!.showTitle(title)
                player2?.showTitle(title)
            }

            override fun concludeLoading() {
                player1!!.resetTitle()
                player2?.resetTitle()
            }

            override suspend fun announceMatchup() {
                val startingTitle = Title.title(
                    Component.empty(),
                    Format.mm(
                        "<white><player1> vs. <player2></white>",
                        Placeholder.component("player1", Format.formatPlayerName(player1)),
                        Placeholder.component("player2", Format.formatPlayerName(player2))
                    ),
                    Title.Times.times(Tick.of(5), Tick.of(100), Tick.of(0))
                )

                player1!!.showTitle(startingTitle)
                player2?.showTitle(startingTitle)

                delay(1300)
                repeat(3) {
                    val color = when(it) {
                        0 -> NamedTextColor.GREEN
                        1 -> NamedTextColor.YELLOW
                        2 -> NamedTextColor.RED
                        else -> NamedTextColor.WHITE
                    }

                    val title = Title.title(
                        Format.mm("<b>> ${3 - it} <</b>").color(color),
                        Format.mm(
                            "<white><player1> vs. <player2></white>",
                            Placeholder.component("player1", Format.formatPlayerName(player1)),
                            Placeholder.component("player2", Format.formatPlayerName(player2))
                        ),
                        Title.Times.times(Tick.of(0), Tick.of(100), Tick.of(0))
                    )

                    player1.showTitle(title)
                    player2?.showTitle(title)

                    delay(400)
                }

                player1.resetTitle()
                player2?.resetTitle()

                suspendSync {
                    player1.removePotionEffect(PotionEffectType.BLINDNESS)
                    player2?.removePotionEffect(PotionEffectType.BLINDNESS)
                    partyController!!.frozenPlayers.remove(player1)
                    if(player2 != null) partyController.frozenPlayers.remove(player2)
                }
            }
        }

        class TeamMatchup(val partyController: PartyController?, val team1: Team, val team2: Team) : PartyMatchup {
            override fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>) {
                team1.getOnlinePlayers().forEachIndexed { i, it ->
                    it.teleport(spawns1[i % spawns1.size])
                    it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5, 1))
                    partyController!!.frozenPlayers.add(it)
                }

                team2.getOnlinePlayers().forEachIndexed { i, it ->
                    it.teleport(spawns2[i % spawns1.size])
                    it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5, 1))
                    partyController!!.frozenPlayers.add(it)
                }
            }

            override fun kitPlayers(kit: Kit.KitDefinition) {
                Kit.giveKits(team1.getOnlinePlayers() + team2.getOnlinePlayers(), kit)
            }

            override fun announceLoading() {
                val title = Title.title(
                    Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                    Component.text("Loading...", NamedTextColor.AQUA),
                    Title.Times.times(Tick.of(0), Tick.of(9999999), Tick.of(0))
                )

                team1.audience.showTitle(title)
                team2.audience.showTitle(title)
            }

            override fun concludeLoading() {
                team1.audience.resetTitle()
                team2.audience.resetTitle()
            }

            override suspend fun announceMatchup() {
                val startingTitle = Title.title(
                    Component.empty(),
                    Format.mm(
                        "<white><team1> vs. <team2></white>",
                        Placeholder.component("team1", team1.formattedName),
                        Placeholder.component("team2", team2.formattedName)
                    )
                )

                team1.audience.showTitle(startingTitle)
                team2.audience.showTitle(startingTitle)

                delay(1000)
                repeat(3) {
                    val color = when(it) {
                        0 -> NamedTextColor.GREEN
                        1 -> NamedTextColor.YELLOW
                        2 -> NamedTextColor.RED
                        else -> NamedTextColor.WHITE
                    }

                    val title = Title.title(
                        Format.mm("<b>> ${3 - it} <</b>").color(color),
                        Format.mm(
                            "<white><team1> vs. <team2></white>",
                            Placeholder.component("team1", team1.formattedName),
                            Placeholder.component("team2", team2.formattedName)
                        )
                    )

                    team1.audience.showTitle(title)
                    team2.audience.showTitle(title)

                    delay(400)
                }

                suspendSync {
                    (team1.getOnlinePlayers() + team2.getOnlinePlayers()).forEach {
                        it.removePotionEffect(PotionEffectType.BLINDNESS)
                        partyController!!.frozenPlayers.remove(it)
                    }
                }
            }
        }
    }

    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        if(event.player in frozenPlayers) event.isCancelled = true
    }
}