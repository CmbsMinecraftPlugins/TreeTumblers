package xyz.devcmb.tumblers.controllers.games.breach

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Sound
import org.bukkit.attribute.Attribute
import org.bukkit.command.CommandSender
import org.bukkit.entity.Item
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.block.Action
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.fill
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.hideToAll
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.showToAll
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
    cutsceneSteps = arrayListOf(),
    flags = setOf(
        Flag.DISABLE_BLOCK_BREAKING,
    ),
    icon = Component.text("\uEA00"),
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
            return loadedMaps.getOrNull(currentRound - 1) ?: loadedMaps[0]
        }

    lateinit var playingTeams: Pair<Team, Team>
    val eventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }
    var team1score: Int = 0
    var team2score: Int = 0
    var currentRound: Int = 1
    var gameState: GameState = GameState.KIT_SELECT

    var team1holder: Player? = null
    var team2holder: Player? = null

    var team1droppedStar: Item? = null
    var team2droppedStar: Item? = null

    var chosenKits: HashMap<Player, BreachKit> = hashMapOf()
    var deadPlayers: HashMap<Player, Boolean> = hashMapOf()
    var starPickupTimes: HashMap<Player, Int> = hashMapOf()

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
        name(Format.mm("<light_purple></light_purple>"))
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
                roundEnd(playingTeams.first)
            },
            "win_2" to { sender ->
                roundEnd(playingTeams.second)
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
        val placements = eventController.getEventTeamPlacements()

        val team1 = placements.find { it.second == 1 }?.first ?: throw GameControllerException("No first place team found!")
        val team2 = placements.find {
            DebugUtil.info("${it.first.name}, ${it.second}")
            it.second == 2
        }?.first ?: throw GameControllerException("No second place team found!")

        playingTeams = Pair(team1, team2)

        repeat(rounds) {
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

                    listOf(playingTeams.first, playingTeams.second).forEachIndexed { i, team ->
                        team.getOnlinePlayers().forEach {
                            it.teleport(if (i == 0) team1spawn else team2spawn)
                            it.inventory.setItem(8, kitSelector.clone())
                            it.showToAll()
                            it.health = 1.0
                            it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 1.0

                            it.openHandledInventory("breachKitSelector")
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
        gameParticipants.forEach {
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
            roundStart()
            awaitEnd()
            delay(2500)
            currentRound++

            if (team1score >= bestOf || team2score >= bestOf) break
        }

        tickTask.cancel()
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        val winner = if (team1score >= bestOf) playingTeams.first else playingTeams.second

        gameParticipants.forEach {
            it.showTitle(Title.title(
                Component.text(winner.teamName).color(winner.color).decorate(TextDecoration.BOLD),
                Component.text("IS VICTORIOUS!").decorate(TextDecoration.BOLD),
                Title.Times.times(Tick.of(0), Tick.of(120), Tick.of(40))
            ))
        }

        delay(9000)
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {

    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {

    }

    suspend fun preRound() {
        chosenKits.clear()
        deadPlayers.clear()
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
                it.removePotionEffect(PotionEffectType.DARKNESS)
                it.removePotionEffect(PotionEffectType.SLOWNESS)
                it.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = it.getAttribute(Attribute.JUMP_STRENGTH)!!.defaultValue
            }
            playingTeams.second.getOnlinePlayers().forEach {
                it.inventory.clear()
                it.inventory.setItem(8, kitSelector.clone())
                it.removePotionEffect(PotionEffectType.DARKNESS)
                it.removePotionEffect(PotionEffectType.SLOWNESS)
                it.getAttribute(Attribute.JUMP_STRENGTH)?.baseValue = it.getAttribute(Attribute.JUMP_STRENGTH)!!.defaultValue
            }
        }

        spawn(SpawnCycle.PRE_ROUND)
        countdown(15, "kit_selection")

        if (team1holder == null && playingTeams.first.getOnlinePlayers().isNotEmpty()) team1holder = playingTeams.first.getOnlinePlayers().random()
        if (team2holder == null && playingTeams.second.getOnlinePlayers().isNotEmpty()) team2holder = playingTeams.second.getOnlinePlayers().random()

        playingTeams.first.getOnlinePlayers().forEach {
            if (chosenKits.get(it) == null) {
                chosenKits[it] = BreachKit.entries.random()
            }

            giveKit(it, chosenKits[it]!!, true)
        }

        playingTeams.second.getOnlinePlayers().forEach {
            if (chosenKits.get(it) == null) {
                chosenKits[it] = BreachKit.entries.random()
            }

            giveKit(it, chosenKits[it]!!, true)
        }

        gameState = GameState.PRE_ROUND
        asyncCountdown(8, "pre_round")
        delay(3000)
        MiscUtils.titleCountdown(Audience.audience(gamePlayers), Format.mm("Round starts in"), 5)
        gameState = GameState.GAME_ON
    }

    fun roundStart() {
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

        asyncCountdown(30, "round") {
            gameState = GameState.ROUND_OVER
            // todo : THIS SHOULD NOT HAPPEN! do SOMETHING to force rounds to end with a winner
        }
    }

    suspend fun awaitEnd() {
        while(true) {
            if(gameState == GameState.ROUND_OVER) break
            delay(200)
        }
        cancelCountdown()
    }

    fun roundEnd(winner: Team) {
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
            if (team1droppedStar != null) {
                if (it.location.distance(team1droppedStar!!.location) < 2 && it != team1holder) {
                    team1holder = it
                    it.inventory.setItemInOffHand(team1star.clone())
                    team1droppedStar!!.remove()
                }
            }

            if (team2droppedStar != null) {
                if (it.location.distance(team2droppedStar!!.location) < 2) {
                    starPickupTimes[it] = starPickupTimes.getOrDefault(it, 0) + 1
                    sendPickupProgressActionbar(it)
                    if (starPickupTimes[it]!! > starPickupTicks) {
                        roundEnd(playingTeams.first)
                    }
                } else {
                    starPickupTimes[it] = 0
                    it.sendActionBar(Component.empty())
                }
            }
        }

        playingTeams.second.getOnlinePlayers().forEach {
            if (team2droppedStar != null) {
                if (it.location.distance(team2droppedStar!!.location) < 2 && it != team2holder) {
                    team2holder = it
                    it.inventory.setItemInOffHand(team2star.clone())
                }
            }

            if (team1droppedStar != null) {
                if (it.location.distance(team1droppedStar!!.location) < 2) {
                    starPickupTimes[it] = starPickupTimes.getOrDefault(it, 0) + 1
                    sendPickupProgressActionbar(it)
                    if (starPickupTimes[it]!! > starPickupTicks) {
                        roundEnd(playingTeams.second)
                    }
                } else {
                    starPickupTimes[it] = 0
                    it.sendActionBar(Component.empty())
                }
            }
        }
    }

    fun starDrop(team: Team, location: Location) {
        val message = Component.empty()
            .append(Component.text(team.icon, NamedTextColor.WHITE).font(UserInterfaceUtility.ICONS))
            .append(Component.text(" ${team.teamName}", team.color))
            .append(Format.mm(" have dropped their <light_purple>star!</light_purple>"))
        // As per DevCmb, the correct word is "have", and not "has". keeping this bug in the game would break everything.

        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(message)
        }

        if (team == playingTeams.first) {
            team1droppedStar = currentMap.world.dropItem(location, team1star.clone())
            team1droppedStar?.isGlowing = true

            team1droppedStar?.owner = UUID.randomUUID() // If this lands on DevCmb, I wouldn't even be suprised.
        } else if (team == playingTeams.second) {
            team2droppedStar = currentMap.world.dropItem(location, team2star.clone())
            team2droppedStar?.isGlowing = true
            team2droppedStar?.owner = UUID.randomUUID() // If this lands on DevCmb, I wouldn't even be suprised.  x2
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
    fun breachPlayerDeathEvent(event: PlayerDeathEvent) {
        event.drops.clear()
        event.isCancelled = true
        event.player.inventory.clear()

        deadPlayers[event.player] = true
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

    enum class GameState {
        KIT_SELECT,
        PRE_ROUND,
        GAME_ON,
        ROUND_OVER
    }
}