package xyz.devcmb.tumblers.controllers.games.breach

import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerSwapHandItemsEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.EventController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.enableBossBar
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateLocation

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
        Flag.DISABLE_BLOCK_BREAKING
    ),
    icon = Component.text("\uEA00"),
    logo = Component.text("awesome logo goes here"),
    scores = hashMapOf(),
    scoreboard = "breachScoreboard"
) {
    companion object {
        val kitItemsKey = NamespacedKey("tumbling", "kit_item")
    }

    val currentMap: LoadedMap
        get() {
            return loadedMaps.getOrNull(currentRound - 1) ?: loadedMaps[0]
        }

    lateinit var playingTeams: Pair<Team, Team>
    val eventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }
    val team1score: Int = 0
    val team2score: Int = 0
    val currentRound: Int = 1

    var team1holder: Player? = null
    var team2holder: Player? = null

    var chosenKits: HashMap<Player, BreachKit> = hashMapOf()

    val kitSelector: ItemStack = AdvancedItemStack(Material.COMPASS) {
        name(Component.text("Kit Selector", NamedTextColor.YELLOW))
        persistentDataContainer {
            set(kitItemsKey, PersistentDataType.BOOLEAN, true)
        }
        rightClick { player ->
            player.openHandledInventory("breachKitSelector")
        }
    }.build()

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

        loadMap(maps.random(), 1)
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

                    playingTeams.first.getOnlinePlayers().forEach {
                        it.teleport(team1spawn)
                        it.inventory.setItem(8, kitSelector.clone())

                        it.openHandledInventory("breachKitSelector")
                    }

                    playingTeams.second.getOnlinePlayers().forEach {
                        it.teleport(team2spawn)
                        it.inventory.setItem(8, kitSelector.clone())

                        it.openHandledInventory("breachKitSelector")
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
        }

        repeat(5) {
            preround()
            delay(10000)
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {

    }

    suspend fun preround() {
        chosenKits.clear()
        team1holder = null
        team2holder = null

        playingTeams.first.getOnlinePlayers().forEach {
            it.inventory.clear()
            it.inventory.setItem(8, kitSelector.clone())
        }
        playingTeams.second.getOnlinePlayers().forEach {
            it.inventory.clear()
            it.inventory.setItem(8, kitSelector.clone())
        }

        spawn(SpawnCycle.PRE_ROUND)
        countdown(15, "kit_selection")

        if (team1holder == null) team1holder = playingTeams.first.getOnlinePlayers().random()
        if (team2holder == null) team2holder = playingTeams.first.getOnlinePlayers().random()

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

        asyncCountdown(8, "pre_round")
        delay(3000)
        MiscUtils.titleCountdown(Audience.audience(gamePlayers), Format.mm("Round starts in"), 5)

        // break walls
    }

    fun giveKit(player: Player, kit: BreachKit, removeSelector: Boolean = false) {
        player.giveKit(kit.kit)
        chosenKits[player] = kit

        if (player == team1holder || player == team2holder) {
            player.inventory.setItemInOffHand(ItemStack.of(Material.NETHER_STAR))
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
            player.inventory.setItemInOffHand(ItemStack.of(Material.NETHER_STAR))
        }

        if (team == playingTeams.second) {
            if (team2holder != null) {
                dropItem(player)
                return
            }
            team2holder = player
            player.inventory.setItemInOffHand(ItemStack.of(Material.NETHER_STAR))
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
}