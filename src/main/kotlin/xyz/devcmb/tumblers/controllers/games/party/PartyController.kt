package xyz.devcmb.tumblers.controllers.games.party

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.extent.clipboard.io.ClipboardFormats
import com.sk89q.worldedit.function.operation.Operations
import com.sk89q.worldedit.session.ClipboardHolder
import io.papermc.paper.util.Tick
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.party.games.shared.MaceDuels
import xyz.devcmb.tumblers.controllers.games.party.games.shared.SpearDuels
import xyz.devcmb.tumblers.controllers.games.party.games.shared.StandardAxeDuels
import xyz.devcmb.tumblers.controllers.games.party.games.shared.StandardBowDuels
import xyz.devcmb.tumblers.controllers.games.party.games.shared.StandardSwordDuels
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.Kit
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.disableBossBar
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.showToAll
import xyz.devcmb.tumblers.util.tumblingPlayer
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
 * Individual game ideas:
 * [x] Standard sword duels (stone sword)
 * [x] Standard axe duels (wooden axe)
 * [x] Standard bow duels (crossbow duels, 3-shot kill)
 * [x] Mace duels (wind charge, 1 hit kill, mace)
 * [ ] Sumo (Fist-fight, last man standing wins)
 * [ ] Quickdraw (single shot gun with instakill)
 * [ ] Pillars (navigate a short parkour course to reach the other side)
 * [ ] Pearl fight (knockback sticks and 2 pearls on a small map)
 * [ ] Horseback spear duels (1 hit kill, netherite spears)
 * [ ] Ice boat race (small track, first to complete wins)
 * [ ] Riptide trident race (fastest one to complete a short trident course wins)
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
        CutsceneStep(Format.mm("In this game, <yellow>you</yellow> and <yellow>your team</yellow> will fight in head-to-head <aqua>minigames!</aqua>")) {
            teleportConfig("cutscene.first")
            delay(5000)
        },
        CutsceneStep(Format.mm("This game comes in <aqua>2 parts...</aqua>")) {
            teleportConfig("cutscene.second")
            delay(2500)
        },
        CutsceneStep(Format.mm("You start playing <yellow>individual games</yellow> where you fight one other person.<br>This stage lasts the first <aqua>5m</aqua> of the game.")) {
            teleportConfig("cutscene.third")
            delay(5000)
        },
        CutsceneStep(Format.mm("Then, you will transition to playing <yellow>team games</yellow> where you fight against a whole team.<br>This stage lasts the final <aqua>5m</aqua> of the game.")) {
            teleportConfig("cutscene.fourth")
            delay(5000)
        },
        CutsceneStep(Format.mm("Game range from <aqua>Sword duels</aqua> to <aqua>Mace duels</aqua> and anything in between!<br>While you're waiting for a match, you'll be waiting here.")) {
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Format.mm("<b><green>Good Luck, Have Fun!</green></b>")) {}
    ),
    scores = hashMapOf(
        PartyScoreSource.INDIVIDUAL_GAME_WIN to 80,
        PartyScoreSource.INDIVIDUAL_GAME_DRAW to 40,
        PartyScoreSource.INDIVIDUAL_GAME_LOSE to 10,
        PartyScoreSource.TEAM_GAME_WIN to 240,
        PartyScoreSource.TEAM_GAME_DRAW to 160,
        PartyScoreSource.TEAM_GAME_LOSE to 40
    ),
    flags = setOf(Flag.DISABLE_FALL_DAMAGE, Flag.DISABLE_BLOCK_BREAKING),
    icon = Component.text("\uEA00").font(NamespacedKey("tumbling", "games/deathrun")),
    scoreboard = "partyScoreboard"
) {
    data class PartyGameIdentifier(val id: String)
    data class PartyGameSchematic(val file: File)

    companion object {
        val games: ArrayList<Class<out PartyGame>> = arrayListOf(
            StandardSwordDuels::class.java,
            StandardAxeDuels::class.java,
            StandardBowDuels::class.java,
            MaceDuels::class.java,
            SpearDuels::class.java
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

        @field:Configurable("games.party.allow_solos")
        var allowSolos: Boolean = false
    }

    override val scoreMessages: HashMap<ScoreSource, (Int) -> Component> = hashMapOf(
        PartyScoreSource.INDIVIDUAL_GAME_WIN to {
            gameMessage(Format.mm("<white>Game won! <gold>[+${it}]</gold></white>"))
        },
        PartyScoreSource.TEAM_GAME_WIN to {
            gameMessage(Format.mm("<white>Game won! <gold>[+${it}]</gold></white>"))
        },
        PartyScoreSource.INDIVIDUAL_GAME_DRAW to {
            gameMessage(Format.mm("<white>Game drawn! <gold>[+${it}]</gold></white>"))
        },
        PartyScoreSource.TEAM_GAME_DRAW to {
            gameMessage(Format.mm("<white>Game drawn! <gold>[+${it}]</gold></white>"))
        },
        PartyScoreSource.INDIVIDUAL_GAME_LOSE to {
            gameMessage(Format.mm("<white>Game drawn! <gold>[+${it}]</gold></white>"))
        },
        PartyScoreSource.TEAM_GAME_LOSE to {
            gameMessage(Format.mm("<white>Game lost! <gold>[+${it}]</gold></white>"))
        }
    )

    lateinit var map: LoadedMap

    lateinit var pivot: Location
    var nextXPosition: Int = 0
    val activeGames: ArrayList<PartyGame> = ArrayList()
    var currentGameType = PartyGameType.INDIVIDUAL

    val waitingIndividualPlayers: MutableSet<Player> = HashSet()
    val waitingTeams: MutableSet<Team> = HashSet()
    val waitingTeamPlayers: HashMap<Team, MutableSet<Player>> = HashMap()
    val inGamePlayers: MutableSet<Player> = HashSet()
    val disabledGameWaitingPlayers: MutableSet<Player> = HashSet()

    val lastIndividualMatchups: HashMap<Player, Player> = HashMap()
    val lastTeamMatchups: HashMap<Team, Team> = HashMap()

    val gameOutcomes: HashMap<Player, ArrayList<PartyGameResult>> = HashMap()

    val frozenPlayers: MutableSet<Player> = HashSet()

    var teamGamesTimer: Timer? = null
    var actionBarRunnable: BukkitRunnable? = null

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

        Team.entries.forEach {
            waitingTeamPlayers[it] = HashSet()
        }

        gameParticipants.forEach {
            gameOutcomes.put(it, ArrayList())
        }

        pivot = pivotLocation
        nextXPosition = pivotLocation.x.toInt()

        actionBarRunnable = object : BukkitRunnable() {
            override fun run() {
                if(currentState != State.GAME_ON) return

                gameParticipants.forEach {
                    val message: Component = if(currentGameType == PartyGameType.GAME_OVER) {
                        Format.mm("<red>Game Over!</red>")
                    } else if(it in disabledGameWaitingPlayers) {
                        Format.mm("<yellow>Waiting for <b>team games</b> to activate...</yellow>")
                    } else if (it in waitingTeamPlayers[it.tumblingPlayer.team]!!) {
                        Format.mm("<aqua>Waiting for your teammates to finish their games...</aqua>")
                    } else if(it !in inGamePlayers) {
                        Format.mm("<aqua>Waiting for a match...</aqua>")
                    } else {
                        Component.empty()
                    }

                    it.sendActionBar(message)
                }
            }
        }
        actionBarRunnable!!.runTaskTimer(TreeTumblers.plugin, 0, 10)
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
            gameParticipants.forEach {
                it.enableBossBar("countdownBossbar")
                spawnPlayer(it, true)
            }
        }
    }

    fun spawnPlayer(player: Player, preGame: Boolean) {
        val spawn = map.data.getList("pregame_spawn")
            ?.validateLocation(map.world)
            ?: throw GameControllerException("Game world does does not contain a valid pregame spawn")

        player.teleport(spawn)
        if(!preGame) {
            player.hideToAll()
            player.isFlying = false
            player.heal(20.0)
            player.inventory.clear()
            player.allowFlight = false
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
        gameParticipants.shuffled().forEach {
            addWaitingPlayer(it)
        }

        teamGamesTimer = Timer(5 * 60) {
            id = "party_team_games_switchover_timer"
            timeBroadcast(
                20,
                "Individual games are going to switch to team games in 20 seconds! Game starting has been disabled!",
                Format.MessageFormatter.GAME_MESSAGE
            ) {
                currentGameType = PartyGameType.DISABLED
            }

            onComplete { early ->
                if(early || currentGameType == PartyGameType.GAME_OVER) return@onComplete

                currentGameType = PartyGameType.TEAM
                disabledGameWaitingPlayers.forEach {
                    addWaitingTeamPlayer(it)
                }
                disabledGameWaitingPlayers.clear()
                Bukkit.broadcast(gameMessage(Component.text("Team games are now active!")))
            }
        }
        teamGamesTimer!!.start()

        countdown(10 * 60)

        currentGameType = PartyGameType.GAME_OVER
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        gameParticipants.forEach {
            it.inventory.clear()
        }

        activeGames.forEach {
            endGame(it, null)
        }

        val placements = getTeamPlacements()
        gameParticipants.forEach { plr ->
            val teamPlacement = placements.find { it.first == plr.tumblingPlayer.team }!!.second

            val color = when(teamPlacement) {
                1 -> NamedTextColor.GOLD
                2 -> TextColor.fromHexString("#E0E0E0")
                3 -> TextColor.fromHexString("#CE8946")
                else -> NamedTextColor.AQUA
            }

            plr.showTitle(Title.title(
                Component.text("Game Over!", NamedTextColor.RED).decorate(TextDecoration.BOLD),
                Component.text("$teamPlacement${MiscUtils.getOrdinalSuffix(teamPlacement)} place!", color),
                Title.Times.times(Tick.of(3), Tick.of(90), Tick.of(3))
            ))
            plr.sendMessage(gameMessage(Component.text("Game Over!")))
        }

        delay(5000)
        announceTeamScores()
        delay(5000)
        announceIndivScores()

        delay(5000)
        announceOverallTeamScores()
        delay(5000)
    }

    override suspend fun cleanup() {
        actionBarRunnable?.cancel()
        Bukkit.getOnlinePlayers().forEach {
            it.disableBossBar("countdownBossbar")
        }

        super.cleanup()
    }

    fun addWaitingPlayer(player: Player) {
        if(currentGameType != PartyGameType.INDIVIDUAL) throw GameControllerException("Attempted to add player to player waitlist while party was not in individual mode")

        DebugUtil.info("Adding ${player.name} to the individual wait queue")
        waitingIndividualPlayers.add(player)

        val opponent = try {
            waitingIndividualPlayers.first {
                it != player
                && it.tumblingPlayer.team != player.tumblingPlayer.team
                && lastIndividualMatchups[player] != it
            }
        } catch(e: NoSuchElementException) {
            null
        }

        if(opponent != null || allowSolos) {
            DebugUtil.info("Found opponent for ${player.name}: ${opponent?.name}")

            waitingIndividualPlayers.remove(player)
            waitingIndividualPlayers.remove(opponent)

            inGamePlayers.add(player)
            opponent?.let { inGamePlayers.add(it) }

            // without this, it would load multiple arenas at the same point
            nextXPosition += 80

            TreeTumblers.pluginScope.async {
                runGame(
                    PartyMatchup.IndividualMatchup(this@PartyController, player, opponent),
                    individualGames.random()
                )
            }
        }
    }

    fun addWaitingTeamPlayer(player: Player) {
        if(currentGameType != PartyGameType.TEAM) throw GameControllerException("Attempted to add player to team waitlist while party was not in team mode")

        val team = player.tumblingPlayer.team
        if((waitingTeamPlayers[team]!!.size + 1) >= team.getOnlinePlayers().size) {
            addWaitingTeam(team)
            waitingTeamPlayers[team]!!.clear()
        } else {
            waitingTeamPlayers[team]!!.add(player)
        }
    }

    fun addWaitingTeam(team: Team) {
        waitingTeams.add(team)
        if(waitingTeams.size >= 2 || allowSolos) {
            val opponent =
                if(allowSolos) Team.entries.filter { it != team && it.playingTeam }.random()
                else waitingTeams.first { it != team }

            waitingTeams.remove(team)
            waitingTeams.remove(opponent)

            nextXPosition += 80

            TreeTumblers.pluginScope.async {
                runGame(PartyMatchup.TeamMatchup(this@PartyController, team, opponent), teamGames.random())
            }
        }
    }

    suspend fun runGame(matchup: PartyMatchup, game: Class<out PartyGame>) {
        matchup.announceLoading()

        if(matchup is PartyMatchup.IndividualMatchup && matchup.player1 != null && matchup.player2 != null) {
            lastIndividualMatchups.put(matchup.player1, matchup.player2)
            lastIndividualMatchups.put(matchup.player2, matchup.player1)
        } else if(matchup is PartyMatchup.TeamMatchup) {
            lastTeamMatchups.put(matchup.team1, matchup.team2)
            lastTeamMatchups.put(matchup.team2, matchup.team1)
        }

        val gameClass = game
            .getDeclaredConstructor(PartyController::class.java, PartyMatchup::class.java)
            .newInstance(this, matchup)

        Bukkit.getPluginManager().registerEvents(gameClass, TreeTumblers.plugin)
        activeGames.add(gameClass)

        if(
            (currentGameType == PartyGameType.INDIVIDUAL && !gameClass.individual)
            || (currentGameType == PartyGameType.TEAM && !gameClass.team)
            || currentGameType == PartyGameType.DISABLED
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

        val pos = BukkitAdapter.adapt(pivot).toBlockPoint().withX(nextXPosition)

        val editSession = WorldEdit.getInstance()
            .newEditSessionBuilder()
            .world(BukkitAdapter.adapt(map.world))
            .fastMode(true)
            .build()

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
            matchup.kitPlayers(gameClass.kit)
            gameClass.postSpawn()
        }

        matchup.concludeLoading()
        matchup.announceMatchup()
        gameClass.start()
    }

    fun endGame(game: PartyGame, winningSide: PartyGame.MatchupSide?) = TreeTumblers.pluginScope.launch {
        when(game.matchup) {
            is PartyMatchup.IndividualMatchup -> {
                gameOutcomes[game.matchup.player1]?.add(
                    when (winningSide) {
                        PartyGame.MatchupSide.FIRST -> PartyGameResult.WIN
                        PartyGame.MatchupSide.SECOND -> PartyGameResult.LOSS
                        else -> PartyGameResult.DRAW
                    }
                )

                gameOutcomes[game.matchup.player2]?.add(
                    when (winningSide) {
                        PartyGame.MatchupSide.FIRST -> PartyGameResult.LOSS
                        PartyGame.MatchupSide.SECOND -> PartyGameResult.WIN
                        else -> PartyGameResult.DRAW
                    }
                )
            }
            is PartyMatchup.TeamMatchup -> {
                game.matchup.players.forEach {
                    gameOutcomes[it]?.add(
                        when (winningSide) {
                            PartyGame.MatchupSide.FIRST -> (if(it.tumblingPlayer.team == game.matchup.team1) PartyGameResult.WIN else PartyGameResult.LOSS)
                            PartyGame.MatchupSide.SECOND -> (if(it.tumblingPlayer.team == game.matchup.team1) PartyGameResult.LOSS else PartyGameResult.WIN)
                            else -> PartyGameResult.DRAW
                        }
                    )
                }
            }
        }
        delay(2000)

        if(!activeGames.contains(game)) return@launch

        game.cleanup()
        activeGames.remove(game)

        HandlerList.unregisterAll(game)
        suspendSync {
            game.matchup.players.forEach {
                inGamePlayers.remove(it)
                spawnPlayer(it, false)
            }
        }

        delay(2000)
        game.matchup.players.forEach {
            if(currentGameType == PartyGameType.INDIVIDUAL) {
                addWaitingPlayer(it)
            } else if(currentGameType == PartyGameType.TEAM) {
                if(game.matchup is PartyMatchup.IndividualMatchup) {
                    addWaitingTeamPlayer(it)
                } else {
                    if(waitingTeams.contains(it.tumblingPlayer.team)) return@forEach
                    addWaitingTeam(it.tumblingPlayer.team)
                }
            } else if(currentGameType == PartyGameType.DISABLED) {
                disabledGameWaitingPlayers.add(it)
            }
        }
    }

    enum class PartyGameType {
        INDIVIDUAL,
        DISABLED,
        TEAM,
        GAME_OVER
    }

    sealed interface PartyMatchup {
        val players: Set<Player>
        fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>)
        fun kitPlayers(kit: Kit.KitDefinition)
        fun announceLoading()
        suspend fun concludeLoading()
        suspend fun announceMatchup()

        class IndividualMatchup(val partyController: PartyController?, val player1: Player?, val player2: Player?) : PartyMatchup {
            override val players: Set<Player> = listOfNotNull(player1, player2).toSet()

            override fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>) {
                player1!!.teleport(spawns1.first())
                player1.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 255))
                player2?.teleport(spawns2.first())
                player2?.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5 * 20, 255))

                player1.heal(20.0)
                player2?.heal(20.0)

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
                    Component.text("Loading arena...", NamedTextColor.GREEN),
                    Title.Times.times(Tick.of(10), Tick.of(9999999), Tick.of(0))
                )

                player1!!.showTitle(title)
                player2?.showTitle(title)
            }

            override suspend fun concludeLoading() {
                val title = Title.title(
                    Component.text("\uE000").font(NamespacedKey("tumbling", "hud")),
                    Component.text("Loading arena...", NamedTextColor.GREEN),
                    Title.Times.times(Tick.of(0), Tick.of(0), Tick.of(10))
                )

                player1!!.showTitle(title)
                player2?.showTitle(title)

                player1.showToAll()
                player2?.showToAll()

                delay(1000)
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
            override val players: Set<Player> = setOf(*team1.getOnlinePlayers().toTypedArray(), *team2.getOnlinePlayers().toTypedArray())

            override fun spawn(spawns1: ArrayList<Location>, spawns2: ArrayList<Location>) {
                team1.getOnlinePlayers().forEachIndexed { i, it ->
                    it.teleport(spawns1[i % spawns1.size])
                    it.heal(20.0)
                    it.addPotionEffect(PotionEffect(PotionEffectType.BLINDNESS, 5, 1))
                    partyController!!.frozenPlayers.add(it)
                }

                team2.getOnlinePlayers().forEachIndexed { i, it ->
                    it.teleport(spawns2[i % spawns1.size])
                    it.heal(20.0)
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

            override suspend fun concludeLoading() {
                team1.audience.resetTitle()
                team2.audience.resetTitle()

                (team1.getOnlinePlayers() + team2.getOnlinePlayers()).forEach {
                    it.showToAll()
                }

                delay(1000)
            }

            override suspend fun announceMatchup() {
                val startingTitle = Title.title(
                    Component.empty(),
                    Format.mm(
                        "<white><team1> vs. <team2></white>",
                        Placeholder.component("team1", team1.formattedName),
                        Placeholder.component("team2", team2.formattedName)
                    ),
                    Title.Times.times(Tick.of(5), Tick.of(100), Tick.of(0))
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
                        ),
                        Title.Times.times(Tick.of(0), Tick.of(100), Tick.of(0))
                    )

                    team1.audience.showTitle(title)
                    team2.audience.showTitle(title)

                    delay(400)
                }

                Audience.audience(team1.audience, team2.audience).resetTitle()

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
        if(
            event.player in frozenPlayers
            && (event.to.x != event.from.x || event.to.z != event.from.z)
        ) event.isCancelled = true
    }

    @EventHandler
    fun entityDamageEvent(event: EntityDamageByEntityEvent) {
        if(event.damager !in inGamePlayers || event.entity !in inGamePlayers) event.isCancelled = true
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        val player = event.player
        val bow: ItemStack = player.inventory.itemInMainHand
        val action = event.action

        if (((action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) && (bow.type == Material.BOW || bow.type == Material.CROSSBOW)) && frozenPlayers.contains(player)) {
            event.isCancelled = true
        }
    }

    enum class PartyScoreSource(override val id: String) : ScoreSource {
        INDIVIDUAL_GAME_WIN("individual_party_game_win"),
        INDIVIDUAL_GAME_LOSE("individual_party_game_lose"),
        INDIVIDUAL_GAME_DRAW("individual_party_game_draw"),
        TEAM_GAME_WIN("team_party_game_win"),
        TEAM_GAME_LOSE("team_party_game_lose"),
        TEAM_GAME_DRAW("team_party_game_draw")
    }

    enum class PartyGameResult {
        WIN,
        LOSS,
        DRAW
    }
}