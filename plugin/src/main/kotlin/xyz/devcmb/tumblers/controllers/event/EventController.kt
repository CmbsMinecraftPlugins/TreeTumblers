package xyz.devcmb.tumblers.controllers.event

import com.destroystokyo.paper.profile.ProfileProperty
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.papermc.paper.datacomponent.item.ResolvableProfile
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.entity.Mannequin
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerInteractEntityEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.server.ServerListPingEvent
import org.bukkit.persistence.PersistentDataType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingException
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.controllers.server.WorldController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.events.LoggedOnTumblingPlayerReadyEvent
import xyz.devcmb.tumblers.ui.MiniMessagePlaceholders
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.calculatePlacements
import xyz.devcmb.tumblers.util.configurable
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.getOrdinalSuffix
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateLocation
import java.util.UUID
import kotlin.math.min

@Controller(Controller.Priority.MEDIUM)
object EventController : IController {
    lateinit var teamScores: HashMap<Team, Int>

    lateinit var topbarRunnable: BukkitRunnable
    var state: State = State.EVENT_INACTIVE

    var eventTimer: Timer? = null

    val readyCheckWaiting: ArrayList<Player> = ArrayList()
    var readyCheckAborted: Boolean = false
    var readyCheckTimer: Timer? = null

    var game: Int = 0
    val totalGames: Int
        get() {
            return min(GameController.games.filter { it.data.votable }.size, 8)
        }
    val playedGames: ArrayList<String> = ArrayList()

    var scoresHidden: Boolean = false
        set(value) {
            field = value
            sendDefaultTopbar()
        }

    var lastGameTeamPlacements: ArrayList<Pair<Team, Int>>? = null
    var lastGamePlayerPlacements: ArrayList<Pair<TumblingPlayer, Int>>? = null
    var lastGameTeamScores: HashMap<Team, Int>? = null
    var lastGamePlayerScores: HashMap<TumblingPlayer, Int>? = null

    val eventMode: Boolean = configurable("event.event_mode")

    val overrideMotd: Boolean = configurable("event.override_motd")
    val skipIntermission: Boolean = configurable("event.intermission.skip")
    val intermissionLength: Int = configurable("event.intermission.time")

    object Podiums {
        val podiumYaw: Double = configurable("event.podiums.yaw")
        val podiumPitch: Double = configurable("event.podiums.pitch")
        val firstPodium: List<Int> = configurable("event.podiums.first")
        val secondPodium: List<Int> = configurable("event.podiums.second")
        val thirdPodiums: List<Int> = configurable("event.podiums.third")
        val individualPodium: List<Int> = configurable("event.podiums.individual")
    }

    object Leaderboards {
        val lastGameTeamPosition: List<Double> = configurable("event.leaderboards.last_game_team")
        val lastGameIndividualPosition: List<Double> = configurable("event.leaderboards.last_game_indiv")
        val overallTeamPosition: List<Double> = configurable("event.leaderboards.overall_team")
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            teamScores = DatabaseController.getTeamScores()
        }

        topbarRunnable = object : BukkitRunnable() {
            override fun run() {
                if(GameController.activeGame != null) {
                    sendGameTopbar()
                } else {
                    sendDefaultTopbar()
                }
            }
        }
        topbarRunnable.runTaskTimer(TreeTumblers.plugin, 0, 20)
    }

    fun startEvent(finale: Boolean, skipIntro: Boolean) {
        TreeTumblers.pluginScope.launch {
            val ready = readyCheck()
            if(!ready) return@launch

            if(finale) game = totalGames

            if(!skipIntro) {
                state = State.PRE_EVENT
                eventTimer = Timer(64) {
                    id = "pre_event_timer"
                    title = "Event Start"
                }
                eventTimer!!.start()

                delay(3000)
                eventStartSequence()
            }

            startEventLoop()
        }
    }

    suspend fun startEventLoop() {
        repeat(totalGames) {
            eventLoop()
        }

        finale()
        cleanupEvent()
    }

    var actionBarTask: BukkitRunnable? = null
    val attribution: ArrayList<Pair<Component, Component>> = arrayListOf(
        Format.mm("<red><b>DevCmb</b></red>") to Format.mm("<white><yellow>Project Lead</yellow> • <red>Lead Programmer</red> • <light_purple>Art</light_purple></white>"),
        Format.mm("<light_purple><b>Nibbl_z</b></light_purple>") to Format.mm("<white><red>Programmer</red> • <light_purple>Composer</light_purple> • <aqua>Builder</aqua></white>"),
        Format.mm("<b><red>Mat</red><white>Mart</white></b>") to Format.mm("<white><color:#ff9100>Game Design</color> • <aqua>Builder</aqua></white>"),
        Format.mm("<color:#ff5cd9><b>TheMasked_Panda</b></color>") to Format.mm("<white><aqua>Builder</aqua> • <light_purple>Art</light_purple></white>"),
    )
    suspend fun eventStartSequence() {
        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<green><b>Tree Tumblers</b></green>"),
            Component.empty(),
            Title.Times.times(Tick.of(0), Tick.of(99999), Tick.of(0))
        ))

        delay(1000)
        // TODO: Play intro music
        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<green><b>Tree Tumblers</b></green>"),
            Format.mm("Is starting in <b><aqua>60 seconds</aqua></b>"),
            Title.Times.times(Tick.of(0), Tick.of(60), Tick.of(20))
        ))

        actionBarTask = object : BukkitRunnable() {
            override fun run() {
                Audience.audience(Bukkit.getOnlinePlayers()).sendActionBar(
                    Format.mm("<green><b>Tree Tumblers</b></green> is starting in <aqua>${eventTimer?.currentTime ?: 0}</aqua> seconds")
                )
            }
        }
        actionBarTask!!.runTaskTimer(TreeTumblers.plugin, 0, 10)

        delay(4000)

        attribution.forEachIndexed { index, (person, work) ->
            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                Title.title(
                person,
                work,
                Title.Times.times(
                    Tick.of(if(index == 0) 10 else 0),
                    Tick.of(if(index == attribution.size - 1) 60 else 9999),
                    Tick.of(10)
                )
            ))

            delay(3000)
        }

        while(eventTimer?.isRunning == true) {
            delay(100)
        }

        actionBarTask!!.cancel()
        actionBarTask = null

        // TODO: Tutorial section
        Bukkit.broadcast(Format.mm("<white><green><line:30></green><br><br>" +
                "Welcome to <green><b>Tree Tumblers</b></green><br>" +
                "Let's meet our <aqua>teams!</aqua><br><br>" +
                "<green><line:30></green></white>"
        ))
        PlayerController.muteChat()
        delay(2500)
        VotingController.announceTeamPlayers()
        delay(1000)
        Bukkit.broadcast(Format.mm("<white><green><line:30></green><br><br>" +
                "And now that everyone has been introduced, let's get this show on the road!<br><br>" +
                "<green><line:30></green></white>"
        ))
        delay(1000)
        PlayerController.unmuteChat()
    }

    fun cleanupEvent() {
        state = State.EVENT_INACTIVE
        playedGames.clear()
        game = 0
        eventTimer = null
    }

    suspend fun eventLoop() {
        if(game >= totalGames) return
        // TODO: Something else for handling finale recovery
        DatabaseController.saveEventState()
        game++

        if(game != 1 && !skipIntermission) {
            state = State.INTERMISSION
            eventTimer = Timer(intermissionLength) {
                id = "event_intermission_timer"
                joined = true
                title = "Intermission"
            }
            eventTimer!!.start()
        }

        if(game == totalGames) {
            scoresHidden = true
            Bukkit.broadcast(Format.info("Scores have been hidden for the final game!"))
        }

        state = State.VOTING
        val nextGame = VotingController.startVoting()
        state = State.NORMAL_GAME
        GameController.startGame(nextGame)
        playedGames.add(nextGame)
    }

    suspend fun finale() {
        eventTimer = Timer(5) {
            id = "score_breakdown_timer"
            joined = true
            title = "Score breakdown"
        }
        eventTimer!!.start()

        PlayerController.muteChat()
        Bukkit.broadcast(
            Format.mm("<green><line:30></green><br>" +
                "<white>That's all for this <b><green>Tree Tumblers</green></b> event!<br>" +
                "Now it's time to see who moves on to the finale!</white><br>" +
                "<green><line:30></green>"
        ))

        delay(5000)
        // TODO: Maybe go through the top 10 indiv
        announceNonFinaleParticipants()

        delay(1000)
        Bukkit.broadcast(
            Format.mm(
            "<br>".repeat(15) +
            "And now...let's reveal our <gold>finalists!</gold>" +
            "<br>".repeat(5)
        ))

        delay(4000)

        val placements = getEventTeamPlacements()
        val first = placements[0].first
        val second = placements[1].first

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<gold><b>FINALISTS</b></gold>"),
            Component.empty(),
            Title.Times.times(Tick.of(0), Tick.of(999999), Tick.of(0))
        ))

        delay(3000)
        Bukkit.broadcast(
            Format.mm(
            "<br>".repeat(15) +
            "First, with <gold>${teamScores[first]}</gold> score..." +
            "<br>".repeat(5)
        ))

        delay(3000)

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<gold><b>FINALISTS</b></gold>"),
            Format.mm("<team1>", Placeholder.component("team1", first.formattedName)),
            Title.Times.times(Tick.of(0), Tick.of(999999), Tick.of(0))
        ))

        Bukkit.broadcast(
            Format.mm(
            "<br>".repeat(15) +
            "First, with <gold>${teamScores[first]}</gold> score...<br>" +
            "<team1>" +
            "<br>".repeat(4),
            Placeholder.component("team1", first.formattedName)
        ))

        delay(4000)

        Bukkit.broadcast(
            Format.mm(
            "<br>".repeat(15) +
                    "Secondly, with <gold>${teamScores[second]}</gold> score..." +
                    "<br>".repeat(5)
        ))

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<gold><b>FINALISTS</b></gold>"),
            Format.mm("<team1> <white>vs.</white>", Placeholder.component("team1", first.formattedName)),
            Title.Times.times(Tick.of(0), Tick.of(999999), Tick.of(0))
        ))

        delay(3000)

        Bukkit.broadcast(
            Format.mm(
            "<br>".repeat(15) +
                    "Secondly, with <gold>${teamScores[second]}</gold> score...<br>" +
                    "<team2>" +
                    "<br>".repeat(4),
            Placeholder.component("team2", second.formattedName)
        ))

        Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
            Title.title(
            Format.mm("<gold><b>FINALISTS</b></gold>"),
            Format.mm(
                "<team1> <white>vs.</white> <team2>",
                Placeholder.component("team1", first.formattedName),
                Placeholder.component("team2", second.formattedName)
            ),
            Title.Times.times(Tick.of(0), Tick.of(80), Tick.of(20))
        ))

        scoresHidden = false

        delay(5000)
        // TODO: Unhardcode?
        Bukkit.broadcast(
            Format.mm(
            "<green><line:30></green><br><br>" +
                "Thank you all so much for participating <red>❤</red><br>" +
                "Now, onto our finale, <gold><b>Breach!</b></gold><br><br>" +
                "<green><line:30></green>"
        ))


        PlayerController.unmuteChat()
        eventTimer = Timer(60) {
            id = "event_finale_countdown"
            joined = true
            title = "Finale"
        }
        eventTimer!!.start()

        state = State.FINAL_GAME
        GameController.startGame("breach")
    }

    suspend fun announceNonFinaleParticipants() {
        val placementsDone: ArrayList<Int> = ArrayList()
        val placements = getReverseEventTeamPlacements()

        // this is very unoptimized
        // welcome to crunch time
        placements.dropLast(3).forEach { (team, placement) ->
            // teams that have already been announced as tied with another team
            if(placement in placementsDone) return@forEach
            placementsDone.add(placement)

            Bukkit.broadcast(
                Format.mm(
                "<br>".repeat(15) +
                        "In ${placement}${getOrdinalSuffix(placement)} place..." +
                        "<br>".repeat(5)
            ))
            Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                Title.title(
                Component.empty(),
                Format.mm("In ${placement}${getOrdinalSuffix(placement)} place"),
                Title.Times.times(Tick.of(0), Tick.of(9999), Tick.of(0))
            ))

            delay(2000)

            val ties = placements.filter { it.second == placement && team != it.first }
            if(ties.isNotEmpty()) {
                val teams = ties.joinToString(" & ") { "<${it.first.name.lowercase()}>" }
                    .let { if(it.isEmpty()) "<${team.name.lowercase()}>" else "<${team.name.lowercase()}> & $it" }
                Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                    Title.title(
                    Format.mm(
                        teams,
                        *Team.entries
                            .filter { it.playingTeam }
                            .map { Placeholder.component(it.name.lowercase(), it.formattedIcon) }
                            .toTypedArray()
                    ),
                    Format.mm("In ${placement}${getOrdinalSuffix(placement)} place"),
                    Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
                ))

                Bukkit.broadcast(
                    Format.mm(
                    "<br>".repeat(15) +
                            "In ${placement}${getOrdinalSuffix(placement)} place...<br>" +
                            teams +
                            "<br>".repeat(4),
                    *Team.entries
                        .filter { it.playingTeam }
                        .map { Placeholder.component(
                            it.name.lowercase(),
                            it.formattedName
                        ) }
                        .toTypedArray()
                ))

                delay(1000)
                Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                    Title.title(
                    Format.mm(
                        teams,
                        *Team.entries
                            .filter { it.playingTeam }
                            .map { Placeholder.component(
                                it.name.lowercase(),
                                it.formattedIcon)
                            }
                            .toTypedArray()
                    ),
                    Format.mm("With <gold>${teamScores[team] ?: 0}</gold> score!"),
                    Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
                ))
                Bukkit.broadcast(
                    Format.mm(
                    "<br>".repeat(15) +
                            "In ${placement}${getOrdinalSuffix(placement)} place...<br>" +
                            "$teams<br>" +
                            "With <gold>${teamScores[team] ?: 0}</gold> score!" +
                            "<br>".repeat(3),
                    *Team.entries
                        .filter { it.playingTeam }
                        .map { Placeholder.component(
                            it.name.lowercase(),
                            it.formattedName
                        ) }
                        .toTypedArray()
                ))
            } else {
                Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                    Title.title(
                    team.formattedName,
                    Format.mm("In ${placement}${getOrdinalSuffix(placement)} place"),
                    Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
                ))
                Bukkit.broadcast(
                    Format.mm(
                    "<br>".repeat(15) +
                            "In ${placement}${getOrdinalSuffix(placement)} place...<br>" +
                            "<team:${team.name}:name>" +
                            "<br>".repeat(4)
                ))

                delay(1000)

                Audience.audience(Bukkit.getOnlinePlayers()).showTitle(
                    Title.title(
                    team.formattedName,
                    Format.mm("With <gold>${teamScores[team] ?: 0}</gold> score!"),
                    Title.Times.times(Tick.of(0), Tick.of(70), Tick.of(5))
                ))
                Bukkit.broadcast(
                    Format.mm(
                    "<br>".repeat(15) +
                            "In ${placement}${getOrdinalSuffix(placement)} place...<br>" +
                            "<team:${team.name}:name><br>" +
                            "With <gold>${teamScores[team] ?: 0}</gold> score!"+
                            "<br>".repeat(3)
                ))
            }

            delay(4000)
        }
    }

    suspend fun readyCheck(): Boolean {
        Bukkit.broadcast(Format.mm("<green><b>Are you ready?</b><green>"))
        Team.entries
            .filter { it.playingTeam }
            .forEach {
                readyCheckWaiting.addAll(it.getOnlinePlayers())
            }

        suspendSync {
            readyCheckWaiting.forEach { it.openHandledInventory("readyCheckInventory") }
        }

        readyCheckTimer = Timer(20) {
            id = "ready_check_expiry"
            onComplete { early ->
                if (early) return@onComplete

                readyCheckWaiting.forEach {
                    Bukkit.broadcast(
                        Format.error(
                            Format.mm(
                                "<player> is not ready!",
                                Placeholder.component("player", it.formattedName)
                            )
                        )
                    )
                }
                readyCheckWaiting.clear()
                readyCheckAborted = true
            }
        }
        readyCheckTimer!!.start()

        while(readyCheckWaiting.isNotEmpty() && !readyCheckAborted) {
            delay(50)
        }

        val aborted = readyCheckAborted
        readyCheckAborted = false

        readyCheckTimer?.end()
        readyCheckTimer = null

        if(aborted) {
            suspendSync {
                Bukkit.getOnlinePlayers().forEach { it.closeInventory() }
            }
            Bukkit.broadcast(Format.warning("Not all players ready! Ready check failed!"))
        } else {
            Bukkit.broadcast(Format.success("All players ready! Ready check success!"))
        }

        return !aborted
    }

    fun markReady(player: Player) {
        Bukkit.broadcast(
            Format.mm(
                "<green><player> is ready!</green>",
                Placeholder.component("player", player.formattedName)
            )
        )
        readyCheckWaiting.remove(player)
    }

    fun markNotReady(player: Player) {
        Bukkit.broadcast(
            Format.mm(
                "<red><player> is not ready!</red>",
                Placeholder.component("player", player.formattedName)
            )
        )
        readyCheckAborted = true
        readyCheckWaiting.clear()
    }

    fun sendDefaultTopbar() {
        val teamComponent = getTopbarPlayersComponent()

        Bukkit.getOnlinePlayers().forEach {
            it.sendPlayerListHeader(
                Component.empty()
                    .appendNewline()
                    .append(Format.mm("<glyph:hud/title>"))
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .append(Format.mm("<green><line:40></green>"))
                    .appendNewline()
                    .append(teamComponent)
                    .appendNewline()
                    .append(Format.mm("<green><line:40></green>"))
            )

            it.sendPlayerListFooter(
                Format.mm(
                "<aqua>Ping: <white>${it.ping}ms</white></aqua><br><aqua>Online Players: <white>${Bukkit.getOnlinePlayers().size}</white></aqua><br>"
            ))
        }
    }

    fun sendGameTopbar() {
        val game = GameController.activeGame ?: return
        val component = game.overrideTabList() ?: getTopbarPlayersComponent(game)

        Bukkit.getOnlinePlayers().forEach {
            it.sendPlayerListHeader(
                Component.empty()
                    .appendNewline()
                    .append(game.data.tabLogo)
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .appendNewline()
                    .append(Format.mm("<green>Game ${this.game}/$totalGames</green>"))
                    .appendNewline()
                    .append(Format.mm("<green><line:40></green>"))
                    .appendNewline()
                    .append(component)
                    .appendNewline()
                    .append(Format.mm("<green><line:40></green>"))
            )

            it.sendPlayerListFooter(
                Format.mm(
                "<aqua>Ping: <white>${it.ping}ms</white></aqua><br><aqua>Online Players: <white>${Bukkit.getOnlinePlayers().size}</white></aqua><br>"
            ))
        }
    }

    fun getTopbarPlayersComponent(game: AbstractGame? = null): Component {
        var teamComponent = Component.empty()

        val placements = game?.getTeamPlacements() ?: getEventTeamPlacements()
        val scores = game?.teamScores ?: teamScores

        if(scoresHidden) {
            repeat(Team.entries.filter { it.playingTeam }.size) {
                var playersComponent = Component.text(" ")
                repeat(4) { index ->
                    val uuid = "606e2ff0-ed77-4842-9d6c-e1d3321c7838"
                    var name = Component.empty()
                    if(index != 0) {
                        name = name.append(Component.text(" • ", NamedTextColor.WHITE))
                    }

                    name = name.append(Format.mm("<white><head:$uuid></white> <dark_gray><obf>Player</obf><dark_gray>"))

                    playersComponent = playersComponent.append(name)
                }
                playersComponent = playersComponent.append(Component.text(" "))

                teamComponent = teamComponent.append(
                    Format.mm(
                        " <br><white>#?</white> <team><shift>${" ".repeat(60)}<gray>?????</gray> <br><players><br>",
                        Placeholder.parsed("team", "<white><font:${TreeTumblers.NAMESPACE}:icons>\uE007</font></white> <dark_gray>????????</dark_gray>"),
                        // By negative spacing, I don't need to do any math for the repetition (and don't need to use periods)
                        Placeholder.component("shift", UserInterfaceUtility.negativeSpace(UserInterfaceUtility.getPixelWidth("????????") + 11)),
                        Placeholder.component("players", playersComponent)
                    )
                )
            }
        } else {
            placements.forEachIndexed { _, it ->
                var playersComponent = getTeamPlayersComponent(it.first)
                playersComponent = playersComponent.append(Component.text(" "))

                teamComponent = teamComponent.append(
                    Format.mm(
                        " <br><white>#${it.second}</white> <team><shift>${" ".repeat(60)}<gold>${scores[it.first]!!}</gold> <br><players><br>",
                        Placeholder.component("team", it.first.formattedName),
                        // By negative spacing, I don't need to do any math for the repetition (and don't need to use periods)
                        Placeholder.component("shift", UserInterfaceUtility.negativeSpace(UserInterfaceUtility.getPixelWidth(it.first.teamName) + 11)),
                        Placeholder.component("players", playersComponent)
                    )
                )
            }
        }

        return teamComponent
    }

    fun getTeamPlayersComponent(team: Team): Component {
        var playersComponent = Component.empty()
        PlayerController.players.filter { entry -> entry.team == team }.forEachIndexed { index, player ->
            val uuid = player.uuid
            val onlinePlayer = Bukkit.getPlayer(uuid)
            var name = Component.empty()
            if(index != 0) {
                name = name.append(Component.text(" •", NamedTextColor.WHITE))
            }

            name = name.append(Format.mm(" <color:${if(onlinePlayer != null) "white" else "dark_gray"}><head:$uuid></color> ")).append(
                if(onlinePlayer != null) Component.text(onlinePlayer.name, team.color)
                else Component.text(player.name, NamedTextColor.GRAY)
            )

            playersComponent = playersComponent.append(name)
        }

        return playersComponent
    }

    fun updateEventScores(game: AbstractGame) {
        DebugUtil.info("Updating team scores from event game")
        game.playerScores.forEach {
            it.key.score += it.value
        }

        game.teamScores.forEach {
            game.teamScores[it.key] = (teamScores[it.key] ?: 0) + it.value
        }
    }

    fun getEventTeamPlacements(): ArrayList<Pair<Team, Int>> {
        val sorted = teamScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<Team, Int>> { it.value }
                .thenBy { it.key.priority }
        )
        return calculatePlacements(sorted)
    }

    fun getReverseEventTeamPlacements(): ArrayList<Pair<Team, Int>> {
        return getEventTeamPlacements().apply { reverse() }
    }

    fun getEventPlayerPlacements(): ArrayList<Pair<TumblingPlayer, Int>> {
        val playerScores: HashMap<TumblingPlayer, Int> = HashMap()
        PlayerController.players.filter { it.team.playingTeam }.forEach {
            playerScores[it] = it.score
        }

        val sorted = playerScores.entries.sortedWith(
            compareBy({ -it.value }, { it.key.team.priority }),
        )
        return calculatePlacements(sorted)
    }

    fun replicateScores() {
        if(!eventMode) return

        TreeTumblers.pluginScope.launch {
            DebugUtil.info("Replicating event data...")
            DatabaseController.replicateTeamData(teamScores)
            PlayerController.players.forEach {
                DatabaseController.replicatePlayerData(it)
            }
            DebugUtil.success("Data replication successful!")
        }
    }

    val mannequins: ArrayList<Mannequin> = ArrayList()
    val playerSpecificMannequins: HashMap<Player, ArrayList<Mannequin>> = HashMap()
    val scoreMannequins: ArrayList<Mannequin> = ArrayList()

    val textDisplays: ArrayList<TextDisplay> = ArrayList()
    val playerSpecificMannequinNameTags: HashMap<Player, ArrayList<TextDisplay>> = HashMap()

    val skinResponseCache: HashMap<TumblingPlayer, SkinDataResponse> = HashMap()

    fun refreshLeaderboards() {
        mannequins.forEach {
            it.remove()
        }
        mannequins.clear()

        textDisplays.forEach {
            it.remove()
        }
        textDisplays.clear()

        scoreMannequins.clear()

        refreshPodiums()
        refreshLastGameTeamScoreboard()
        refreshLastGameIndividualScoreboard()
        refreshOverallTeamScoreboard()
    }

    fun refreshPodiums() {
        val placements = getEventPlayerPlacements()
        val top = placements.take(3)

        val podiums = arrayListOf(Podiums.firstPodium, Podiums.secondPodium, Podiums.thirdPodiums)
        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!
        top.forEachIndexed { i, placement ->
            val podium = podiums[i].validateLocation(hub)
                ?: throw TumblingException("Podium ${i + 1} position is not valid!")

            val pos = podium.toCenterLocation()
            pos.y = podium.y
            pos.yaw = Podiums.podiumYaw.toFloat()
            pos.pitch = Podiums.podiumPitch.toFloat()

            spawnScoreMannequin(pos, placement)
        }

        val individualPosition = Podiums.individualPodium.validateLocation(hub)
            ?: throw TumblingException("Individual podium position is not valid!")

        val pos = individualPosition.toCenterLocation()
        pos.y = individualPosition.y

        Bukkit.getOnlinePlayers().forEach {
            spawnPlayerIndividualMannequin(it)
        }
    }

    fun refreshLastGameTeamScoreboard() {
        if(lastGameTeamPlacements == null || lastGameTeamScores == null) return

        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!
        val startPos = Leaderboards.lastGameTeamPosition.validateLocation(hub)
            ?: throw TumblingException("Individual scoreboard position is not valid!")

        if(!startPos.chunk.isLoaded) startPos.chunk.load()

        lastGameTeamPlacements!!.reversed().forEachIndexed { i, placement ->
            hub.spawn(startPos.clone().add(0.0, 0.3 * i, 0.0), TextDisplay::class.java) {
                it.persistentDataContainer.set(
                    WorldController.temporaryEntityKey,
                    PersistentDataType.BOOLEAN,
                    true
                )
                textDisplays.add(it)

                it.text(Format.mm(
                    if(!scoresHidden) MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT
                        else MiniMessagePlaceholders.Game.HIDDEN_TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.parsed("placement", "<b>${placement.second}</b>"),
                    Placeholder.component("team", placement.first.formattedName),
                    Placeholder.unparsed("score", (lastGameTeamScores!![placement.first] ?: 0).toString())
                ))
            }
        }

        hub.spawn(startPos.clone().add(0.0, 0.3 * lastGameTeamPlacements!!.size, 0.0), TextDisplay::class.java) {
            it.persistentDataContainer.set(
                WorldController.temporaryEntityKey,
                PersistentDataType.BOOLEAN,
                true
            )

            textDisplays.add(it)
            it.text(Format.mm("<white><b>Last game team placements</b></white>"))
        }
    }

    fun refreshLastGameIndividualScoreboard() {
        if(lastGamePlayerPlacements == null || lastGamePlayerScores == null) return

        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!
        val startPos = Leaderboards.lastGameIndividualPosition.validateLocation(hub)
            ?: throw TumblingException("Individual scoreboard position is not valid!")

        if(!startPos.chunk.isLoaded) startPos.chunk.load()

        // use team count for consistency
        val playingTeams = Team.entries.filter { it.playingTeam }.size
        lastGamePlayerPlacements!!
            .take(playingTeams) // don't reverse since its top-to-bottom
            .forEachIndexed { i, placement ->
                val pos = startPos.clone().add(0.0, 0.3 * (playingTeams - (i + 1)), 0.0)
                hub.spawn(pos, TextDisplay::class.java) {
                    textDisplays.add(it)
                    it.text(Format.mm(
                        if(!scoresHidden) MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT_NO_HEAD
                            else MiniMessagePlaceholders.Game.HIDDEN_INDIVIDUAL_SCOREBOARD_PLACEMENT_NO_HEAD,
                        Placeholder.parsed("placement", "<b>${placement.second}</b>"),
                        Placeholder.component("player", placement.first.formattedName),
                        Placeholder.unparsed("score", (lastGamePlayerScores!![placement.first] ?: 0).toString())
                    ))
                }
            }

        hub.spawn(startPos.clone().add(0.0, 0.3 * playingTeams, 0.0), TextDisplay::class.java) {
            textDisplays.add(it)
            it.text(Format.mm("<white><b>Last game player placements</b></white>"))
        }
    }

    fun refreshOverallTeamScoreboard() {
        if(!teamScores.any { it.value != 0 }) return

        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!
        val startPos = Leaderboards.overallTeamPosition.validateLocation(hub)
            ?: throw TumblingException("Individual scoreboard position is not valid!")

        if(!startPos.chunk.isLoaded) startPos.chunk.load()

        val placements = getEventTeamPlacements()
        placements.reversed().forEachIndexed { i, placement ->
            hub.spawn(startPos.clone().add(0.0, 0.3 * i, 0.0), TextDisplay::class.java) {
                it.persistentDataContainer.set(
                    WorldController.temporaryEntityKey,
                    PersistentDataType.BOOLEAN,
                    true
                )
                textDisplays.add(it)
                it.text(Format.mm(
                        if(!scoresHidden) MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT
                        else MiniMessagePlaceholders.Game.HIDDEN_TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.parsed("placement", "<b>${placement.second}</b>"),
                    Placeholder.component("team", placement.first.formattedName),
                    Placeholder.unparsed("score", (teamScores[placement.first] ?: 0).toString())
                ))
            }
        }

        hub.spawn(startPos.clone().add(0.0, 0.3 * placements.size, 0.0), TextDisplay::class.java) {
            it.persistentDataContainer.set(
                WorldController.temporaryEntityKey,
                PersistentDataType.BOOLEAN,
                true
            )
            textDisplays.add(it)
            it.text(Format.mm("<white><b>Overall team placements</b></white>"))
        }
    }

    fun spawnPlayerIndividualMannequin(player: Player) {
        val placements = getEventPlayerPlacements()
        val playerPlacement = placements.find { it.first == player.tumblingPlayer } ?: Pair(player.tumblingPlayer, 0)

        val hub = Bukkit.getWorld(WorldController.lobbyWorld)!!

        val individualPosition = Podiums.individualPodium.validateLocation(hub)
            ?: throw TumblingException("Individual podium position is not valid!")

        val pos = individualPosition.toCenterLocation()
        pos.y = individualPosition.y

        if(!pos.chunk.isLoaded) pos.chunk.load()

        val (npc, _, offset) = spawnScoreMannequin(pos, playerPlacement, player)
        scoreMannequins.add(npc)

        hub.spawn(pos.clone().add(0.0,offset,0.0), TextDisplay::class.java) { display ->
            display.persistentDataContainer.set(
                WorldController.temporaryEntityKey,
                PersistentDataType.BOOLEAN,
                true
            )
            display.isVisibleByDefault = false
            player.showEntity(TreeTumblers.plugin, display)

            textDisplays.add(display)
            playerSpecificMannequinNameTags[player]!!.add(display)

            display.text(Format.mm("<green>> View placements <</green>"))
        }
    }

    @Suppress("UnstableApiUsage")
    fun spawnScoreMannequin(location: Location, placement: Pair<TumblingPlayer, Int>, player: Player? = null): Triple<Mannequin, ArrayList<TextDisplay>, Double> {
        val npc = location.world.spawn(location, Mannequin::class.java) { npc ->
            mannequins.add(npc)
            npc.persistentDataContainer.set(
                WorldController.temporaryEntityKey,
                PersistentDataType.BOOLEAN,
                true
            )
            npc.isInvulnerable = true
            npc.isImmovable = true

            if(player != null) {
                npc.isVisibleByDefault = false
                player.showEntity(TreeTumblers.plugin, npc)
                playerSpecificMannequins[player]!!.add(npc)
            }

            val player = placement.first
            TreeTumblers.pluginScope.launch {
                val data = getSkinData(player)
                val skin = data.properties.first()

                val uuid = UUID.randomUUID()
                val profile = Bukkit.createProfile(uuid, uuid.toString().take(16))
                profile.properties.add(
                    ProfileProperty("textures", skin.value, skin.signature)
                )

                suspendSync {
                    npc.profile = ResolvableProfile.resolvableProfile(profile)
                }
            }
        }

        val displays: ArrayList<TextDisplay> = arrayListOf()
        var offset = 2.0

        if(player == null || player.tumblingPlayer.team.playingTeam) {
            displays.add(location.world.spawn(location.clone().add(0.0,offset,0.0), TextDisplay::class.java) {
                if(player != null) {
                    it.isVisibleByDefault = false
                    player.showEntity(TreeTumblers.plugin, it)
                    playerSpecificMannequinNameTags[player]!!.add(it)
                }

                it.text(Format.mm("<white><gold>${if(scoresHidden) "????" else placement.first.score}</gold> score</white>"))
            })

            offset += 0.3
        }

        displays.add(location.world.spawn(location.clone().add(0.0,offset,0.0), TextDisplay::class.java) {
            if(player != null) {
                it.isVisibleByDefault = false
                player.showEntity(TreeTumblers.plugin, it)
                playerSpecificMannequinNameTags[player]!!.add(it)
            }

            it.text(placement.first.formattedName)
        })
        offset += 0.3

        if(player == null || player.tumblingPlayer.team.playingTeam) {
            displays.add(location.world.spawn(location.clone().add(0.0,offset,0.0), TextDisplay::class.java) {
                if(player != null) {
                    it.isVisibleByDefault = false
                    player.showEntity(TreeTumblers.plugin, it)
                    playerSpecificMannequinNameTags[player]!!.add(it)
                }

                it.text(Format.mm("<bold>#${if(scoresHidden) "?" else placement.second}</bold>"))
            })
            offset += 0.3
        }

        displays.forEach {
            it.persistentDataContainer.set(
                WorldController.temporaryEntityKey,
                PersistentDataType.BOOLEAN,
                true
            )
        }
        textDisplays.addAll(displays)

        return Triple(npc, displays, offset)
    }

    suspend fun getSkinData(player: TumblingPlayer): SkinDataResponse {
        if(skinResponseCache.containsKey(player)) return skinResponseCache[player]!!

        return TreeTumblers.httpClient.get(
            "https://sessionserver.mojang.com/session/minecraft/profile/${player.uuid}?unsigned=false"
        ).body<SkinDataResponse>()
    }

    @Serializable
    data class SkinDataResponse(
        val id: String,
        val name: String,
        val properties: List<SkinDataResponseProperty>
    ) {
        @Serializable
        data class SkinDataResponseProperty(
            val name: String,
            val value: String,
            val signature: String
        )
    }

    override fun cleanup() {
        replicateScores()
    }

    @EventHandler
    fun serverListPingEvent(event: ServerListPingEvent) {
        if(!overrideMotd) return

        event.motd(
            Format.mm(
            "<b><green>Tree Tumblers</green> <white>•</white> <gold>Event Server</gold></b><br>" +
                    "<aqua>${GameController.games.filter { it.data.votable }.size} games</aqua> <dark_gray>|</dark_gray> ${if(Constants.IS_DEVELOPMENT) "<gold>${Constants.BRANCH}</gold>" else "<green>production</green>"} <dark_gray>|</dark_gray> <gray>v${TreeTumblers.plugin.pluginMeta.version} (${Constants.VERSION})</gray>",
        ))
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun playerReadyEvent(event: LoggedOnTumblingPlayerReadyEvent) {
        val player = event.bukkitPlayer
        playerSpecificMannequins[player] = arrayListOf()
        playerSpecificMannequinNameTags[player] = arrayListOf()

        spawnPlayerIndividualMannequin(player)
    }

    @EventHandler
    fun playerLeaveEvent(event: PlayerQuitEvent) {
        val player = event.player
        playerSpecificMannequins[player]!!.forEach {
            mannequins.remove(it)
            it.remove()
        }

        playerSpecificMannequinNameTags[player]!!.forEach {
            textDisplays.remove(it)
            it.remove()
        }

        playerSpecificMannequins.remove(player)
        playerSpecificMannequinNameTags.remove(player)
    }

    val debounces: ArrayList<Player> = ArrayList()

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEntityEvent) {
        val player = event.player
        if(player in debounces) return
        if(event.rightClicked in scoreMannequins) {
            debounces.add(player)
            if(scoresHidden) {
                player.sendMessage(Format.warning("Scores are currently hidden!"))
                runTaskLater(20) {
                    debounces.remove(player)
                }
                return
            }

            var message = Component.empty().append(Format.mm("<green><line:30></green>"))
            val placements = getEventPlayerPlacements()
            placements.forEach {
                val plr = it.first
                message = message.append(
                    Format.mm(
                    "<br><white><b>${it.second}.</b></white> <player> <white>-</white> <gold>${plr.score}</gold>",
                    Placeholder.component("player", plr.formattedName)
                ))
            }
            message = message.append(Format.mm("<br><green><line:30></green>"))

            val playerPlacement = placements.find { it.first == player.tumblingPlayer }
            if(playerPlacement != null) {
                message = message.append(
                    Format.mm(
                    "<br><white><b>${playerPlacement.second}.</b></white> <player> <white>-</white> <gold>${player.tumblingPlayer.score}</gold><br><green><line:30></green>",
                    Placeholder.component("player", player.formattedName)
                ))
            }

            player.sendMessage(message)
            runTaskLater(20) {
                debounces.remove(player)
            }
        }
    }

    fun recover(state: DatabaseController.EventRecoveryState) {
        val eventState = state.eventState
        Bukkit.broadcast(Format.warning("The current state of the event is being rolled back, things may lag!"))

        game = eventState.currentGame
        playedGames.clear()
        playedGames.addAll(eventState.playedGames)
        teamScores.clear()
        teamScores.putAll(eventState.teamScores)
        PlayerController.players.forEach { plr -> eventState.playerScores[plr.uuid.toString()]?.let { plr.score = it } }
        lastGameTeamScores = eventState.lastGameTeamScores
        lastGameTeamPlacements = eventState.lastGameTeamPlacements?.let { ArrayList(it) }

        lastGamePlayerScores = eventState.lastGamePlayerScores?.let { HashMap(it.mapNotNull { entry ->
            val player = PlayerController.players.find { plr -> plr.uuid == UUID.fromString(entry.key) }
            player?.let { player to entry.value }
        }.toMap()) }

        lastGamePlayerPlacements = eventState.lastGamePlayerPlacements?.let { ArrayList(it.mapNotNull { entry ->
            val player = PlayerController.players.find { plr -> plr.uuid == UUID.fromString(entry.first) }
            player?.let { player to entry.second }
        }) }

        refreshLeaderboards()

        runBlocking {
            eventState.votingQuadrantGames.forEach {
                VotingController.placeGame(it.key, GameController.games.find { game -> game.data.id == it.value }!!, null)
            }

            Bukkit.broadcast(Format.success("Game state has been rolled back successfully!"))

            eventTimer = Timer(10) {
                id = "recovery_timer"
                paused = true
                joined = true
                title = "Recovery"
            }
            eventTimer!!.start()

            startEventLoop()
        }
    }

    @Serializable
    data class EventState(
        /** Whether or not the event is currently active **/
        val eventActive: Boolean,

        /** The current value of the [currentGame] varaible **/
        val currentGame: Int,

        /** Where the current voting arena has dioramas placed. Will not contain a value for empty quadrant indices **/
        val votingQuadrantGames: HashMap<Int, String>,

        /** A list of all the game ids that have already been played this event **/
        val playedGames: List<String>,

        /** A list of pairs of a [Team] enum to their placement in the last played game **/
        var lastGameTeamPlacements: List<Pair<Team, Int>>?,

        /** A list of pairs of a [TumblingPlayer]'s UUID in string form to their placement in the last played game **/
        var lastGamePlayerPlacements: List<Pair<String, Int>>?,

        /** A hashmap of a [Team] enum to their score in the last played game **/
        var lastGameTeamScores: HashMap<Team, Int>?,

        /** A hashmap of a [TumblingPlayer]'s UUID in string form to their score in the last played game **/
        var lastGamePlayerScores: HashMap<String, Int>?,

        /** A hashmap of each [Team] to their score **/
        val teamScores: HashMap<Team, Int>,

        /** A hashmap of a [TumblingPlayer]'s UUID in string form to their overall score **/
        val playerScores: HashMap<String, Int>
    )

    enum class State {
        EVENT_INACTIVE,
        PRE_EVENT,
        INTERMISSION,
        VOTING,
        NORMAL_GAME,
        FINAL_GAME,
        POST_EVENT
    }
}