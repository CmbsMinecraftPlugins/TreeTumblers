package xyz.devcmb.tumblers.controllers.games.breach

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.FireworkEffect
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.block.Barrel
import org.bukkit.block.Block
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.inventory.InventoryOpenEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerPickupArrowEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.disableBossBar
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.fill
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.randomBetween
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.showToAll
import xyz.devcmb.tumblers.util.sound
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import java.util.UUID
import kotlin.collections.set
import kotlin.math.min

@EventGame
class BreachController: GameBase(
    id = "breach",
    name = "Breach",
    votable = false,
    maps = setOf(
        Map("stadium")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/breach")))
                .append(Component.text(" Breach")),
            "cutscene.start"
        ) { map ->
            delay(5000)
        },
        CutsceneStep(Format.mm("Each round you get to pick a weapon, and one person on the team needs to hold the <light_purple>star.</light_purple>"),
            "cutscene.preround"
        ) { map ->
            delay(5000)
        },
        CutsceneStep(Format.mm("A round is <green>won</green> by stealing the other team's <light_purple>star</light_purple>, by <red>killing</red> the holder of the <light_purple>star</light_purple> and picking it up before their teammates can retrieve it"),
            "cutscene.star"
        ) { map ->
            delay(1000)
            val starLocation = getLocation("cutscene.star_spawn")
            lateinit var star: Item
            suspendSync {
                star = map.world.dropItem(starLocation, ItemStack.of(Material.NETHER_STAR))
                star.isGlowing = true
            }
            delay(4000)
            suspendSync {
                star.remove()
            }
        },
        CutsceneStep(Format.mm("As the round goes on, walls will start to crack and break down, opening up the map"),
            "cutscene.breaking"
        ) { map ->
            val game = game as BreachController
            var breakingTask = object : BukkitRunnable() {
                override fun run() {
                    game.breakBlock()
                }
            }

            breakingTask.runTaskTimer(TreeTumblers.plugin, 0, 3)

            runTaskLater(9 * 20) {
                breakingTask.cancel()
            }

            delay(5000)
        },
        CutsceneStep(Format.mm("The first team to win 3 rounds, <green>wins it all.</green>"),
            "cutscene.end"
        ) { map ->
            delay(4000)
        },
        CutsceneStep(Format.mm("<b><gold>Good Luck, Have Fun, and may the best team win!</gold></b>"),
            "cutscene.end"
        ) { map ->
            // A comment from "Nibbles"
            // Ts Pmo Why Don It Work Pmo Pmo Pmo Pmo

//            val game = game as BreachController
//            val color = game.playingTeams.first.color
//
//            val team1spawn = getLocation("team_1_spawn").clone()
//            team1spawn.setRotation(0f, 0f)
//
//            suspendSync {
//                DebugUtil.info("$team1spawn")
//                MiscUtils.spawnFirework(team1spawn, FireworkEffect.builder()
//                    .trail(false)
//                    .flicker(true)
//                    .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                    .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                    .with(FireworkEffect.Type.STAR)
//                    .build()
//                )
//            }
            //delay(2000)
        },
//        CutsceneStep(Format.mm("<b><green>Have Fun.</green></b>"),
//            "cutscene.end"
//        ) { map ->
//            val game = game as BreachController
//            val color = game.playingTeams.second.color
//
//            val team2spawn = getLocation("team_2_spawn")
//
//            suspendSync {
//                MiscUtils.spawnFirework(
//                    team2spawn, FireworkEffect.builder()
//                        .trail(false)
//                        .flicker(true)
//                        .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                        .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                        .with(FireworkEffect.Type.STAR)
//                        .build()
//                )
//            }
//            delay(2000)
//        },
    ),
    flags = setOf(
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION,
        Flag.DISABLE_FALL_DAMAGE
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("awesome logo goes here"),
    scores = hashMapOf(),
    scoreboard = "breachScoreboard"
) {
    companion object {
        val font = NamespacedKey("tumbling", "games/breach")
        val kitItemsKey = NamespacedKey("tumbling", "kit_item")
        val team1starKey = NamespacedKey("tumbling", "breach_star_1")
        val team2starKey = NamespacedKey("tumbling", "breach_star_2")

        @field:Configurable("games.breach.best_of")
        var bestOf: Int = 3

        val rounds: Int
            get() { return (bestOf * 2) - 1 }

        @field:Configurable("game.breach.star_pickup_ticks")
        var starPickupTicks = 60
    }

    val currentMap: LoadedMap
        get() {
            return loadedMaps.getOrNull(currentRound) ?: loadedMaps[0]
        }

    lateinit var playingTeams: Pair<Team, Team>
    val eventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }
    val playerController: PlayerController by lazy {
        ControllerDelegate.getController("playerController") as PlayerController
    }

    var team1score: Int = 0
    var team2score: Int = 0
    var currentRound: Int = 0
    var gameState: GameState = GameState.KIT_SELECT

    var team1holder: Player? = null
    var team2holder: Player? = null

    var team1droppedStar: Item? = null
    var team2droppedStar: Item? = null

    var chosenKits: HashMap<Player, BreachKit> = hashMapOf()
    var deadPlayers: HashMap<Player, Boolean> = hashMapOf()
    var deathLocations: HashMap<Player, Location> = hashMapOf()
    var starPickupTimes: HashMap<Player, Int> = hashMapOf()

    val kills: HashMap<Player, Int> = hashMapOf()
    val deaths: HashMap<Player, Int> = hashMapOf()

    val kitSelector: ItemStack = AdvancedItemStack(Material.COMPASS) {
        name(Component.text("Kit Selector", NamedTextColor.YELLOW))
        persistentDataContainer {
            set(kitItemsKey, PersistentDataType.BOOLEAN, true)
        }
        rightClick { player ->
            player.openHandledInventory("breachKitSelector")
        }
    }.build()

    val team1star: ItemStack = AdvancedItemStack(Material.NETHER_STAR) {
        name(Format.mm("<light_purple>Star</light_purple>"))
        persistentDataContainer {
            set(team1starKey, PersistentDataType.BOOLEAN, true)
        }
    }.build()

    val team2star: ItemStack = AdvancedItemStack(Material.NETHER_STAR) {
        name(Format.mm("<light_purple>Star</light_purple>"))
        persistentDataContainer {
            set(team2starKey, PersistentDataType.BOOLEAN, true)
        }
    }.build()

    override val debugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "star_1" to { sender ->

            },
            "star_2" to { sender ->
                val team2spawn = currentMap.data.getList("team_2_spawn")?.validateLocation(currentMap.world)
                    ?: throw GameControllerException("Team 2 spawn not found")

                starDrop(playingTeams.second, team2spawn)
            },
            "win_1" to { sender ->
                val team1spawn = currentMap.data.getList("team_1_spawn")?.validateLocation(currentMap.world)
                    ?: throw GameControllerException("Team 1 spawn not found")

                roundEnd(playingTeams.first, team1spawn)
            },
            "win_2" to { sender ->
                val team2spawn = currentMap.data.getList("team_2_spawn")?.validateLocation(currentMap.world)
                    ?: throw GameControllerException("Team 2 spawn not found")

                roundEnd(playingTeams.second, team2spawn)
            }
        )

        override fun killEvent(killer: Player?, killed: Player?) {}
        override fun deathEvent(killed: Player?) {}
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        gamePlayers.forEach {
            deaths[it] = 0
            kills[it] = 0
        }

        val placements = eventController.getEventTeamPlacements()

        val team1 = placements.find { it.second == 1 }?.first ?: throw GameControllerException("No first place team found!")
        val team2 = placements.find {
            DebugUtil.info("${it.first.name}, ${it.second}")
            it.second == 2
        }?.first ?: throw GameControllerException("No second place team found!")

        playingTeams = Pair(team1, team2)

        repeat(rounds + 1) {
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
        suspendSync {
            when(cycle) {
                SpawnCycle.PREGAME -> {}
                SpawnCycle.PRE_ROUND -> {
                    val team1spawn = currentMap.data.getList("team_1_spawn")?.validateLocation(currentMap.world)
                        ?: throw GameControllerException("Team 1 spawn not found")

                    val team2spawn = currentMap.data.getList("team_2_spawn")?.validateLocation(currentMap.world)
                        ?: throw GameControllerException("Team 2 spawn not found")

                    val spectatorSpawn = currentMap.data.getList("spectator_spawn")?.validateLocation(currentMap.world)
                        ?: throw GameControllerException("Spectator spawn not found")

                    listOf(playingTeams.first, playingTeams.second).forEachIndexed { i, team ->
                        team.getOnlinePlayers().forEach {
                            it.teleport(if (i == 0) team1spawn else team2spawn)
                            it.inventory.setItem(8, kitSelector.clone())
                            it.health = 1.0
                            it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0
                            it.openHandledInventory("breachKitSelector")
                            val hiddenTeam = playerController.playerUIControllers[it]?.playerScoreboard?.getTeam("hiddenNames")
                            team.getOnlinePlayers().forEach { plr ->
                                hiddenTeam?.addEntry(plr.name)
                            }


                            cleanupPlayer(it)
                        }
                    }

                    gamePlayers.forEach {
                        if (it.tumblingPlayer.team != playingTeams.first && it.tumblingPlayer.team != playingTeams.second) {
                            makeSpectator(it, true, false)
                            it.teleport(spectatorSpawn)
                        }
                    }
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
        currentRound = 1

        gamePlayers.forEach {
            it.enableBossBar("countdownBossbar")
            it.enableBossBar("breachScoreBossbar")
        }

        val tickTask = object : BukkitRunnable() {
            override fun run() {
                if (gameState == GameState.GAME_ON) {
                    checkItemPickup()
                }
            }
        }

        tickTask.runTaskTimer(TreeTumblers.plugin, 1, 1)

        while (true) {
            preRound()
            roundStart(currentRound)
            awaitEnd()
            if (team1score >= bestOf || team2score >= bestOf) break

            delay(2500)
            currentRound++
        }

        tickTask.cancel()
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        val winner = if (team1score >= bestOf) playingTeams.first else playingTeams.second
        gameState = GameState.GAME_OVER

        suspendSync {
            gameParticipants.forEach {
                it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
                it.health = 20.0
                it.sound(Sound.UI_TOAST_CHALLENGE_COMPLETE)
                it.disableBossBar("countdownBossbar")
                it.inventory.clear()
                val hiddenTeam = playerController.playerUIControllers[it]?.playerScoreboard?.getTeam("hiddenNames")
                gameParticipants.forEach { plr ->
                    hiddenTeam?.addEntry(plr.name)
                }
                // #crunch maxxing
                cleanupPlayer(it)
            }
        }


        val bounds = currentMap.data.getList("bounds")?.map {
            if (it !is List<*>) throw GameControllerException("Map bounds is not a valid list")
            it.validateList<Int>() ?: throw GameControllerException("Door locations does not contain exclusively Integers")
        } ?: throw GameControllerException("Map bounds not found")

        val bound1 = bounds[0].validateLocation(currentMap.world) ?: throw GameControllerException("Map bound point 1 is invalid")
        val bound2 = bounds[1].validateLocation(currentMap.world) ?: throw GameControllerException("Map bound point 1 is invalid")

        val types = listOf(
            FireworkEffect.Type.STAR,
            FireworkEffect.Type.BALL_LARGE,
            FireworkEffect.Type.BALL,
            FireworkEffect.Type.BURST,
        )

        val fireworkTask = object : BukkitRunnable() {
            override fun run() {
                MiscUtils.spawnFirework(bound1.randomBetween(bound2).add(0.0,10.0,0.0), FireworkEffect.builder()
                    .trail(true)
                    .flicker(true)
                    .withColor(Color.fromRGB(winner.color.red(), winner.color.green(), winner.color.blue()))
                    .withColor(Color.fromRGB(winner.color.red(), winner.color.green(), winner.color.blue()))
                    .with(types.random())
                    .build(), (10..30).random().toLong()
                )
            }
        }

        fireworkTask.runTaskTimer(TreeTumblers.plugin, 0, 4)

        gameParticipants.forEach {
            it.showTitle(Title.title(
                winner.formattedName,
                Component.text("ARE VICTORIOUS!").decorate(TextDecoration.BOLD),
                Title.Times.times(Tick.of(0), Tick.of(120), Tick.of(40))
            ))
        }

        delay(12000)

        fireworkTask.cancel()

        gameParticipants.forEach {
            it.disableBossBar("breachScoreBossbar")
        }

        delay(4000)
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        player.enableBossBar("countdownBossbar")
        player.enableBossBar("breachScoreBossbar")

        gameParticipants.forEach {
            val hiddenTeam = playerController.playerUIControllers[it]?.playerScoreboard?.getTeam("hiddenNames")
            gameParticipants.forEach { plr ->
                hiddenTeam?.addEntry(plr.name)
            } // You know when.. you.. w....uh..
        }// its getting to me.
        // its painfu.l. its not good.but   works. it wokrs... it works.


        val team = player.tumblingPlayer.team
        val team1spawn = currentMap.data.getList("team_1_spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Team 1 spawn not found")

        val team2spawn = currentMap.data.getList("team_2_spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Team 2 spawn not found")

        val spectatorSpawn = currentMap.data.getList("spectator_spawn")?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Spectator spawn not found")

        if (team == playingTeams.first || team == playingTeams.second) {
            if (deathLocations[player] != null) {
                player.teleport(deathLocations[player]!!)
            } else {
                player.teleport(if (team == playingTeams.first) team1spawn else team2spawn)
            }


            if (gameState == GameState.KIT_SELECT) {
                if (chosenKits.get(player) != null) {
                    giveKit(player, chosenKits[player]!!)
                } else {
                    player.inventory.setItem(8, kitSelector.clone())
                }
            }

            if (gameState == GameState.PRE_ROUND) {
                player.closeInventory()

                if (chosenKits.get(player) == null) {
                    chosenKits[player] = BreachKit.entries.random()
                }

                giveKit(player, chosenKits[player]!!, true)
            }

            player.health = 1.0
            player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0

            if (gameState == GameState.GAME_ON) {
                player.health = 0.0
            }
        } else {
            player.teleport(spectatorSpawn)
            makeSpectator(player, true, false)
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        if ((player == team1holder || player == team2holder) && gameState == GameState.GAME_ON) {
            deathLocations[player] = player.location
            starDrop(player.tumblingPlayer.team, player.location)
        }
    }

    suspend fun preRound() {
        chosenKits.clear()
        deadPlayers.clear()
        deathLocations.clear()
        team1holder = null
        team2holder = null
        suspendSync {
            team1droppedStar?.remove()
            team2droppedStar?.remove()
        }
        team1droppedStar = null
        team2droppedStar = null
        gameState = GameState.KIT_SELECT

        suspendSync {
            playingTeams.first.getOnlinePlayers().forEach {
                it.inventory.clear()
                it.inventory.setItem(8, kitSelector.clone())
            }
            playingTeams.second.getOnlinePlayers().forEach {
                it.inventory.clear()
                it.inventory.setItem(8, kitSelector.clone())
            }
        }

        spawn(SpawnCycle.PRE_ROUND)
        countdown(15, "kit_selection")

        if (team1holder == null && playingTeams.first.getOnlinePlayers().isNotEmpty()) team1holder = playingTeams.first.getOnlinePlayers().random()
        if (team2holder == null && playingTeams.second.getOnlinePlayers().isNotEmpty()) team2holder = playingTeams.second.getOnlinePlayers().random()

        suspendSync {
            playingTeams.first.getOnlinePlayers().forEach {
                it.closeInventory()

                if (chosenKits.get(it) == null) {
                    chosenKits[it] = BreachKit.entries.random()
                }

                giveKit(it, chosenKits[it]!!, true)
            }

            playingTeams.second.getOnlinePlayers().forEach {
                it.closeInventory()

                if (chosenKits.get(it) == null) {
                    chosenKits[it] = BreachKit.entries.random()
                }

                giveKit(it, chosenKits[it]!!, true)
            }
        }

        gameState = GameState.PRE_ROUND
        asyncCountdown(8, "pre_round")
        delay(3000)
        MiscUtils.titleCountdown(Audience.audience(gamePlayers), Format.mm("Round starts in"), 5)
        gameState = GameState.GAME_ON
    }

    fun roundStart(round: Int) {
        val doors: List<List<List<Int>>> = (currentMap.data.getList("doors")?.map { list ->
            if (list !is List<*>) throw GameControllerException("Door locations is not a valid list")
            list.map { it ->
                if (it !is List<*>) throw GameControllerException("Door locations is not a valid list")
                it.validateList<Int>() ?: throw GameControllerException("Door locations does not contain exclusively Integers")
            }
        } ?: throw GameControllerException("Door locations not found"))

        doors.forEach { door ->
            val point1 = door[0].validateLocation(currentMap.world) ?: throw GameControllerException("Door point 1 is an invalid location")
            val point2 = door[1].validateLocation(currentMap.world) ?: throw GameControllerException("Door point 2 is an invalid location")

            var i = 0L

            (point1.y.toInt()..point2.y.toInt()).forEach {
                runTaskLater(i * 5L) {
                    currentMap.world.fill(
                        Location(currentMap.world, point1.x, it.toDouble(), point1.z),
                        Location(currentMap.world, point2.x, it.toDouble(), point2.z),
                        Material.AIR
                    )
                }

                i++
            }
        }


        var breakingTask: BukkitRunnable? = null

        fun resetTask() {
            breakingTask = object : BukkitRunnable() {
                override fun run() {
                    if (gameState == GameState.GAME_ON) {
                        breakBlock()
                    }
                }
            }
        }

        asyncCountdown(150, "round") {
            breakingTask?.cancel()
        }

        runTaskLater(15 * 20) {
            if (currentRound != round || gameState != GameState.GAME_ON) return@runTaskLater
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(gameMessage(Format.mm("The map is starting to deteriorate!")))
            }

            resetTask()
            breakingTask?.runTaskTimer(TreeTumblers.plugin, 0, 10)
        }

        runTaskLater(30 * 20 + 5) {
            if (currentRound != round || gameState != GameState.GAME_ON) return@runTaskLater
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(gameMessage(Format.mm("The map is starting to deteriorate faster!")))
            }
            breakingTask?.cancel()
            resetTask()
            breakingTask?.runTaskTimer(TreeTumblers.plugin, 5, 7)
        }

        runTaskLater(50 * 20 + 6) {
            if (currentRound != round || gameState != GameState.GAME_ON) return@runTaskLater
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(gameMessage(Format.mm("The map is starting to deteriorate even faster!!")))
            }
            breakingTask?.cancel()
            resetTask()
            breakingTask?.runTaskTimer(TreeTumblers.plugin, 5, 5)
        }

        runTaskLater(70 * 20 + 7) {
            if (currentRound != round || gameState != GameState.GAME_ON) return@runTaskLater
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(gameMessage(Format.mm("The map is starting to deteriorate even, EVEN faster!!!")))
            }
            breakingTask?.cancel()
            resetTask()
            breakingTask?.runTaskTimer(TreeTumblers.plugin, 5, 3)
        }

        runTaskLater(150*20) {
            if (currentRound != round || gameState != GameState.GAME_ON) return@runTaskLater
            playingTeams.first.getOnlinePlayers().forEach {
                it.isGlowing = true
            }

            playingTeams.second.getOnlinePlayers().forEach {
                it.isGlowing = true
            }

            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(gameMessage(Format.mm("All players are now glowing!")))
            }
        }
    }

    suspend fun awaitEnd() {
        while(true) {
            if(gameState == GameState.ROUND_OVER) break
            delay(200)
        }
        cancelCountdown()
    }

    fun roundEnd(winner: Team, fireworkPos: Location) {
        lateinit var loser: Team

        if (winner == playingTeams.first) {
            team1score++
            loser = playingTeams.second
        }
        if (winner == playingTeams.second) {
            team2score++
            loser = playingTeams.first
        }

        if (team1score == bestOf || team2score == bestOf) {
            gameState = GameState.ROUND_OVER
            return
        }

        MiscUtils.spawnFirework(fireworkPos.clone().add(0.0,5.0,0.0), FireworkEffect.builder()
            .trail(false)
            .flicker(true)
            .withColor(Color.fromRGB(winner.color.red(), winner.color.green(), winner.color.blue()))
            .withColor(Color.fromRGB(winner.color.red(), winner.color.green(), winner.color.blue()))
            .with(FireworkEffect.Type.STAR)
            .build()
        )

        winner.getOnlinePlayers().forEach {
            it.showTitle(Title.title(
                Format.mm("<b><green>Round Won!</green></b>"),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(40), Tick.of(10))
            ))
        }

        loser.getOnlinePlayers().forEach {
            it.showTitle(Title.title(
                Format.mm("<b><red>Round Lost</red></b>"),
                Component.empty(),
                Title.Times.times(Tick.of(0), Tick.of(40), Tick.of(10))
            ))
        }

        gameState = GameState.ROUND_OVER
    }

    fun cleanupPlayer(player: Player) {
        player.removePotionEffect(PotionEffectType.DARKNESS)
        player.removePotionEffect(PotionEffectType.SLOWNESS)
        player.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = player.getAttribute(Attribute.JUMP_STRENGTH)!!.defaultValue
        player.isGlowing = false
        player.showToAll()
    }

    fun breakBlock() {
        val bounds = currentMap.data.getList("bounds")?.map {
            if (it !is List<*>) throw GameControllerException("Map bounds is not a valid list")
            it.validateList<Int>() ?: throw GameControllerException("Door locations does not contain exclusively Integers")
        } ?: throw GameControllerException("Map bounds not found")

        val bound1 = bounds[0].validateLocation(currentMap.world) ?: throw GameControllerException("Map bound point 1 is invalid")
        val bound2 = bounds[1].validateLocation(currentMap.world) ?: throw GameControllerException("Map bound point 1 is invalid")

        fun pickBlock(): Block {
            val position = bound1.randomBetween(bound2)

            if (position.block.type == Material.AIR) return pickBlock()

            var surroundingAirs = 0
            val surrounding = listOf(
                Vector(1,0,0), Vector(-1,0,0),
                Vector(0,1,0), Vector(0,-1,0),
                Vector(0,0,1), Vector(0,0,-1)
            )

            surrounding.forEach {
                val block = position.clone().add(it).block.type
                if (block == Material.AIR || block == Material.PACKED_MUD) surroundingAirs++
            }

            if (surroundingAirs < 2) return pickBlock()
            return position.block
        }

        val block = pickBlock()

        gameParticipants.forEach {
            it.sendBlockDamage(block.location, 0.5f, (0..1000000).random())
        }

        currentMap.world.spawnParticle(Particle.BLOCK, block.location, 10, 0.05, 0.05, 0.05, block.blockData)
        currentMap.world.playSound(block.location, block.blockData.soundGroup.breakSound, 2f, 0.7f)

        runTaskLater(10L) {
            currentMap.world.spawnParticle(Particle.BLOCK, block.location, 30, 0.05, 0.05, 0.05, block.blockData)
            currentMap.world.playSound(block.location, block.blockData.soundGroup.breakSound, 2f, 1f)
            block.type = Material.AIR
        }
    }

    fun giveKit(player: Player, kit: BreachKit, removeSelector: Boolean = false) {
        player.giveKit(kit.kit)
        chosenKits[player] = kit

        if (player == team1holder) {
            player.inventory.setItemInOffHand(team1star.clone())
        } else if (player == team2holder) {
            player.inventory.setItemInOffHand(team2star.clone())
        }

        if (removeSelector) return
        player.inventory.setItem(8, kitSelector.clone())
    }

    fun takeItem(player: Player) {
        val team = player.tumblingPlayer.team

        if (team == playingTeams.first) {
            if (team1holder != null) {
                dropItem(player)
                return
            }
            team1holder = player
            player.inventory.setItemInOffHand(team1star.clone())
        }

        if (team == playingTeams.second) {
            if (team2holder != null) {
                dropItem(player)
                return
            }
            team2holder = player
            player.inventory.setItemInOffHand(team2star.clone())
        }
    }

    fun dropItem(player: Player) {
        val team = player.tumblingPlayer.team

        if (team == playingTeams.first && team1holder == player) {
            team1holder = null
            player.inventory.setItemInOffHand(ItemStack.empty())
        }

        if (team == playingTeams.second && team2holder == player) {
            team2holder = null
            player.inventory.setItemInOffHand(ItemStack.empty())
        }
    }

    fun sendPickupProgressActionbar(player: Player) {
        val currentTicks = starPickupTimes.getOrDefault(player, 0).toDouble()
        val totalTicks = starPickupTicks.toDouble()
        val progress = min(currentTicks / totalTicks, 1.0)

        var component = Format.mm("<light_purple>Stealing: </light_purple>")

        component = component.append(Format.mm("<white>[</white>"))

        repeat(20) { i ->
            val color = if (progress>= (i.toDouble() / 20.0)) "aqua" else "dark_grey"
            component = component.append(Format.mm("<$color>|</$color>"))
        }
        component = component.append(Format.mm("<white>] ${(progress * 100.0).toInt()}%</white>"))


        player.playSound(player.location, Sound.BLOCK_NOTE_BLOCK_PLING, 5f, 0.5f + progress.toFloat())
        player.sendActionBar(component)
    }

    fun checkItemPickup() {
        playingTeams.first.getOnlinePlayers().forEach {
            if (deadPlayers[it] == true) return@forEach

            if (team1droppedStar != null) {
                if (it.location.distance(team1droppedStar!!.location) < 2 && it != team1holder) {
                    team1holder = it
                    it.inventory.setItemInOffHand(team1star.clone())
                    team1droppedStar!!.remove()
                    team1droppedStar = null
                }
            }

            if (team2droppedStar != null) {
                if (it.location.distance(team2droppedStar!!.location) < 2) {
                    starPickupTimes[it] = starPickupTimes.getOrDefault(it, 0) + 1
                    sendPickupProgressActionbar(it)
                    if (starPickupTimes[it]!! > starPickupTicks) {
                        roundEnd(playingTeams.first, team2droppedStar!!.location)
                    }
                } else {
                    starPickupTimes[it] = 0
                    it.sendActionBar(Component.empty())
                }
            }
        }

        playingTeams.second.getOnlinePlayers().forEach {
            if (deadPlayers[it] == true) return@forEach

            if (team2droppedStar != null) {
                if (it.location.distance(team2droppedStar!!.location) < 2 && it != team2holder) {
                    team2holder = it
                    it.inventory.setItemInOffHand(team2star.clone())
                    team2droppedStar!!.remove()
                    team2droppedStar = null
                }
            }

            if (team1droppedStar != null) {
                if (it.location.distance(team1droppedStar!!.location) < 2) {
                    starPickupTimes[it] = starPickupTimes.getOrDefault(it, 0) + 1
                    sendPickupProgressActionbar(it)
                    if (starPickupTimes[it]!! > starPickupTicks) {
                        roundEnd(playingTeams.second, team1droppedStar!!.location)
                    }
                } else {
                    starPickupTimes[it] = 0
                    it.sendActionBar(Component.empty())
                }
            }
        }
    }

    fun starDrop(team: Team, location: Location) {
        if (team == playingTeams.first) {
            if (team1droppedStar != null) return
            team1droppedStar = currentMap.world.dropItem(location, team1star.clone())
            team1droppedStar?.isGlowing = true

            team1droppedStar?.owner = UUID.randomUUID() // If this lands on DevCmb, I wouldn't even be suprised.
        } else if (team == playingTeams.second) {
            if (team2droppedStar != null) return
            team2droppedStar = currentMap.world.dropItem(location, team2star.clone())
            team2droppedStar?.isGlowing = true
            team2droppedStar?.owner = UUID.randomUUID() // If this lands on DevCmb, I wouldn't even be suprised.  x2
        }

        val message = Component.empty()
            .append(Component.text(team.icon, NamedTextColor.WHITE).font(UserInterfaceUtility.ICONS))
            .append(Component.text(" ${team.teamName}", team.color))
            .append(Format.mm(" have dropped their <light_purple>star!</light_purple>"))
        // As per DevCmb, the correct word is "have", and not "has". keeping this bug in the game would break everything.

        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(message)
        }


    }

    @EventHandler
    fun inventoryClickEvent(event: InventoryClickEvent) {
        if (event.rawSlot == 45 && event.inventory.holder is Player) {
            event.isCancelled = true
        }

        if (event.cursor.type == Material.NETHER_STAR) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerSwapHandItemEvent(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun projectileLaunchEvent(event: ProjectileLaunchEvent) {
        if (gameState == GameState.KIT_SELECT) {
            event.isCancelled = true
            return
        }
    }

    @EventHandler
    fun playerDropItemEvent(event: PlayerDropItemEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        val player = event.player
        val bow: ItemStack = player.inventory.itemInMainHand
        val action = event.action

        if (
            (action == Action.RIGHT_CLICK_AIR || action == Action.RIGHT_CLICK_BLOCK) &&
            (bow.type == Material.BOW || bow.type == Material.CROSSBOW || bow.type == Material.TRIDENT) &&
            gameState == GameState.KIT_SELECT)
        {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerSwapOffhand(event: PlayerSwapHandItemsEvent) {
        event.isCancelled = true
    }

    @EventHandler
    fun barrelOpenEvent(event: InventoryOpenEvent) {
        if (event.inventory.holder is Barrel) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun damageEvent(event: EntityDamageEvent) {
        if (gameState != GameState.GAME_ON) {
            event.isCancelled = true
            return
        }
        if (event.entityType != EntityType.PLAYER) return

        val attacker = event.damageSource.causingEntity as Player
        val victim = event.entity as Player
        if (attacker.tumblingPlayer.team == victim.tumblingPlayer.team) {
            event.isCancelled = true
            return
        }

        if (deadPlayers[attacker] == true) {
            event.isCancelled = true
            return
        }

        if (event.cause == EntityDamageEvent.DamageCause.ENTITY_ATTACK && attacker.inventory.itemInMainHand.type == Material.TRIDENT) {
            event.damage = 10000.0 // death
            kills[attacker] = kills.getOrDefault(attacker, 0) + 1

            return
        }

        if (event.cause == EntityDamageEvent.DamageCause.PROJECTILE) {
            event.damage = 10000.0 // death
            kills[attacker] = kills.getOrDefault(attacker, 0) + 1

            return
        }

        event.isCancelled = true
    }

    @EventHandler
    fun breachPlayerDeathEvent(event: PlayerDeathEvent) {
        event.drops.clear()
        event.isCancelled = true
        event.player.inventory.clear()

        if (deadPlayers[event.player] != true) {
            deaths[event.player] = deaths.getOrDefault(event.player, 0) + 1
        }

        deadPlayers[event.player] = true
        deathLocations[event.player] = event.player.location
        event.player.addPotionEffect(PotionEffect(
            PotionEffectType.DARKNESS,
            PotionEffect.INFINITE_DURATION,
            67,
            false,
            false,
            false
        ))

        event.player.addPotionEffect(PotionEffect(
            PotionEffectType.SLOWNESS,
            PotionEffect.INFINITE_DURATION,
            255,
            false,
            false,
            false
        ))

        event.player.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = 0.0

        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(Format.formatKillMessage(event.player.killer, event.player, it, 0))
        }

        event.player.hideToAll()

        if (event.player == team1holder || event.player == team2holder) {
            starDrop(event.player.tumblingPlayer.team, event.player.location)
        }
    }

    @EventHandler
    fun playerPickupArrowEvent(event: PlayerPickupArrowEvent) {
        if (event.arrow.type != EntityType.ARROW) return
        event.isCancelled = true
    }

    enum class GameState {
        KIT_SELECT,
        PRE_ROUND,
        GAME_ON,
        ROUND_OVER,
        GAME_OVER
    }
}