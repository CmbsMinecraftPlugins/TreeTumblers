package xyz.devcmb.tumblers.controllers.games.tower_ascent

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentData
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentScoreSource
import xyz.devcmb.tumblers.controllers.games.tower_ascent.data.TowerAscentSpawn
import xyz.devcmb.tumblers.controllers.games.tower_ascent.feature.TowerGenerator
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.item.Kit
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.center
import xyz.devcmb.tumblers.util.disableActionBar
import xyz.devcmb.tumblers.util.enableActionBar
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.titleCountdown
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.util.UUID
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@EventGame
class TowerAscentController : AbstractGame(TowerAscentData) {
    val map: LoadedMap
        get() = loadedMaps.first()

    lateinit var generator: TowerGenerator

    val teamRoomSetIndexes: HashMap<Team, Int> = HashMap()
    val teamRooms: HashMap<Team, Int>
        get() = HashMap(generator.towerHandlers.associate { it.team to it.currentRoomIndex })
    val teamCompletedRooms: HashMap<Team, Int> = HashMap()
    val teamRoomPlacements: HashMap<Team, ArrayList<Int>> = HashMap()

    val teamsFinished: ArrayList<Team> = ArrayList()
    val completedPlayers: ArrayList<TumblingPlayer> = ArrayList()

    val playerKit: Kit.KitDefinition = object : Kit.KitDefinition {
        override val items: ArrayList<Kit.KitItem> = arrayListOf(
            Kit.KitItem.StandardItem(ItemStack(Material.IRON_SWORD)),
            Kit.KitItem.StandardItem(ItemStack(Material.IRON_PICKAXE)),
            Kit.KitItem.StandardItem(ItemStack(Material.COOKED_BEEF, 4)),

            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_HELMET)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.IRON_CHESTPLATE)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_LEGGINGS)),
            Kit.KitItem.ArmorItem(ItemStack.of(Material.LEATHER_BOOTS)),
        )
        override val defaultDropability: Boolean = true
        override val uuid: UUID = UUID.randomUUID()

        // since you can get new items and move around your old ones
        // allowing you to save it would make your sword or something randomly move
        override val saveLoadout: Boolean = false
    }

    val playerGoldCounts: HashMap<TumblingPlayer, Int> = HashMap()

    override val scoreMessages: HashMap<ScoreSource, (score: Int) -> Component> = hashMapOf(
        TowerAscentScoreSource.COMPLETE_ROOM to {
            gameMessage(Format.mm("Completed room <gold>[+$it]</gold>"))
        },
        TowerAscentScoreSource.COMPLETE_TOWER to {
            gameMessage(Format.mm("Completed tower <gold>[+$it]</gold>"))
        }
    )

    override val debugToolkit: DebugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "infinite_gold" to event@{ sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("This event can only be executed by players!"))
                    return@event
                }

                playerGoldCounts[sender.tumblingPlayer] = Integer.MAX_VALUE
                sender.sendMessage(Format.success("Gave infinite gold successfully!"))
            }
        )
    }

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
                teamCompletedRooms[team] = 0
                teamRoomPlacements[team] = arrayListOf()

                generator.towerHandlers.getOrNull(index)?.team = team
                generator.mapSpawns.getOrNull(index)?.let {
                    it.wallBounds.first.forEachRegion(it.wallBounds.second) { block ->
                        block.type = team.glass
                    }
                }

                spawnPlayers(map, team.getOnlinePlayers(), TowerAscentSpawn.valueOf("SET_${index + 1}"))
                team.getOnlinePlayers().forEach {
                    it.giveKit(playerKit)
                }
            }
        }
    }

    override suspend fun gamePregame() {
        gameParticipants.forEach {
            it.enableActionBar("towerAscentActionBar")
            alivePlayers.add(it)
        }

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
        generator.towerHandlers.forEach {
            it.startGame()
        }

        timer(Timer(12.minutes) {
            id = "tower_ascent_game_on"
            title = "Game Over"
            joined = true
        })
    }

    var isGameOver = false
    override suspend fun postGame() {
        isGameOver = true
        generator.towerHandlers.forEach { it.endGame() }
        super.postGame()
    }

    override suspend fun cleanup() {
        generator.cleanup()
        gameParticipants.forEach {
            it.disableActionBar("towerAscentActionBar")
        }
        super.cleanup()
    }

    val alivePlayers: ArrayList<TumblingPlayer> = ArrayList()
    fun playerDeath(player: TumblingPlayer) {
        Bukkit.broadcast(Format.mm(
            "<gray>(<white><glyph:icon/skull></white>) <player> was lost in the tower</gray>",
            Placeholder.component("player", player.formattedName)
        ))

        alivePlayers.remove(player)
        playerGoldCounts[player] = 0

        if(player.isOnline) {
            player.bukkitPlayer!!.sendMessage(Format.warning("You will be respawned when the room is cleared or when your entire team has been eliminated."))
        }

        val aliveTeamPlayers = alivePlayers.filter { it.team == player.team }
        if(aliveTeamPlayers.isEmpty()) {
            Bukkit.broadcast(Format.mm(
                "<red>(<white><glyph:icon/skull></white>) <team> <b>were lost in the tower</b></red>",
                Placeholder.component("team", player.team.formattedName)
            ))
            respawnTeam(player.team)
        }
    }

    fun respawnPlayer(player: TumblingPlayer) {
        if(!player.isOnline) {
            DebugUtil.info("Player ${player.name} is not online to respawn")
            return
        }

        alivePlayers.add(player)

        val bukkitPlayer = player.bukkitPlayer!!
        val handler = generator.towerHandlers.find { it.team == player.team }
            ?: throw GameControllerException("Attempted to respawn a player that does not have a tower handler for their team")

        val currentRoom = handler.currentRoom

        unSpectate(bukkitPlayer)
        bukkitPlayer.foodLevel = 20
        bukkitPlayer.saturation = 3f
        bukkitPlayer.giveKit(playerKit)

        val startingLocation = currentRoom.startingElevatorBounds?.center()
            ?: getSpawns(map, TowerAscentSpawn.valueOf("SET_${player.team.ordinal + 1}")).random()

        bukkitPlayer.tp(startingLocation)
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        if(isGameOver) return

        if(!player.tumblingPlayer.team.playingTeam) {
            spawnPlayers(map, listOf(player), TowerAscentSpawn.SET_1)
            return
        }

        val handler = generator.towerHandlers.find { it.team == player.tumblingPlayer.team }
            ?: throw GameControllerException("Attempted to respawn a player that does not have a tower handler for their team")

        if(player.tumblingPlayer in completedPlayers) {
            makeSpectator(player)
            player.tp(handler.endingRoom.startingElevatorBounds.center())
            player.sendMessage(Format.warning("You've already completed the tower and are waiting for the game to end."))
            return
        }

        if(!handler.roomActive) {
            respawnPlayer(player.tumblingPlayer)
        } else {
            makeSpectator(player)

            val currentRoom = handler.currentRoom
            val startingLocation = currentRoom.startingElevatorBounds?.center()
                ?: getSpawns(map, TowerAscentSpawn.valueOf("SET_${player.tumblingPlayer.team.ordinal + 1}")).random()

            player.tp(startingLocation)
            player.sendMessage(Format.warning("You've joined while a room is active and will be respawned whenever the room is cleared, or if your entire team is eliminated."))

            if(alivePlayers.none { it.team == player.tumblingPlayer.team })
                respawnTeam(player.tumblingPlayer.team)
        }
    }

    val respawningTeams: ArrayList<Team> = ArrayList()
    private fun respawnTeam(team: Team) {
        if(team in respawningTeams) return

        respawningTeams.add(team)
        TreeTumblers.pluginScope.launch {
            delay(2.seconds)
            titleCountdown(team.audience, Format.mm("Respawning in..."), 10)

            suspendSync {
                team.getOnlinePlayers().forEach {
                    DebugUtil.info("Respawning ${it.name}")
                    respawnPlayer(it.tumblingPlayer)
                }
            }
            respawningTeams.remove(team)
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        val plr = player.tumblingPlayer
        if(!plr.team.playingTeam || !isGameOver) return

        val handler = generator.towerHandlers.find { it.team == player.tumblingPlayer.team }
            ?: throw GameControllerException("Attempted to respawn a player that does not have a tower handler for their team")
        if(!handler.roomActive || plr !in alivePlayers) return

        playerDeath(plr)
    }

    @EventHandler
    fun towerAscentPlayerDeathEvent(event: PlayerDeathEvent) {
        val player = event.player
        val tumblingPlayer = player.tumblingPlayer
        if(tumblingPlayer !in gameParticipants) return

        playerDeath(tumblingPlayer)
    }
}