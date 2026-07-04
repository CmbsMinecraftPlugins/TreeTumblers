package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.launch
import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.base.RoundedGame
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import kotlin.math.min
import kotlin.time.Duration.Companion.minutes

@EventGame
class BrawlController : RoundedGame(
    BrawlData,
    3,
    5.minutes
) {
    val kitSelector = AdvancedItemStack(Material.COMPASS) {
        name(Format.mm("<yellow>Kit Selector</yellow>"))
        droppable = false
        rightClick {
            it.openHandledInventory("brawlKitSelector")
        }
    }.build()

    val roundKits: ArrayList<ArrayList<BrawlKit>> = ArrayList()
    val playerKits: HashMap<TumblingPlayer, BrawlKit> = HashMap()

    val alivePlayers: ArrayList<TumblingPlayer> = ArrayList()
    val roundPlacements: ArrayList<HashMap<TumblingPlayer, Int>> = ArrayList()

    override suspend fun preRound() {
        playerKits.clear()
        suspendSync {
            val spectators = Team.entries
                .filter { !it.playingTeam }
                .fold(arrayListOf<Player>()) { acc, entry -> acc.apply { addAll(entry.getOnlinePlayers()) } }
            spectators.forEach { makeSpectator(it) }

            spawnPlayers(
                loadedMaps[roundIndex],
                gamePlayers.mapNotNull { it.bukkitPlayer },
                BrawlSpawn.PRE_ROUND
            )

            gameParticipants.mapNotNull { it.bukkitPlayer }.forEach {
                it.inventory.clear()
                it.inventory.addItem(kitSelector)
            }
        }

        timer(Timer(45) {
            id = "brawl_kit_select"
            title = "Kit Select"
            joined = true
        })

        alivePlayers.clear()
        alivePlayers.addAll(gameParticipants)
        gameParticipants.mapNotNull { it.bukkitPlayer }.forEach { it.inventory.clear() }

        suspendSync {
            gameParticipants.forEach {
                if(it !in playerKits.keys) {
                    selectKit(
                        it,
                        getAvailableRandomKit(it),
                        true
                    )
                }

                it.bukkitPlayer?.let { plr ->
                    giveKit(plr)
                    plr.closeInventory()
                }
            }
        }

        super.preRound()
    }

    fun getAvailableRandomKit(player: TumblingPlayer): BrawlKit {
        return roundKits[roundIndex].filter { roundKit ->
            getSelectedKitPlayers(player.team, roundKit).size < 2
        }.random()
    }

    fun getSelectedKitPlayers(team: Team, kit: BrawlKit): List<TumblingPlayer> {
        return playerKits.filter { it.key.team == team && it.value == kit }.map { it.key }.toList()
    }

    override suspend fun startRound() {
        val currentMap = loadedMaps[roundIndex]
        val rooms = currentMap.data.getList("rooms")
            ?.validateList<List<*>>()
            ?.map {
                it.validateList<Number>() ?: throw GameControllerException("Current map does not have valid spawn boxes")
            }
            ?: throw GameControllerException("Current map does not have any spawn boxes")

        suspendSync {
            rooms.forEach { it ->
                val from = it.take(3).validateLocation(currentMap.world)
                    ?: throw GameControllerException("Room $it does not have a valid from position")
                val to = it.drop(3).validateLocation(currentMap.world)
                    ?: throw GameControllerException("Room $it does not have a valid to position")

                from.forEachRegion(to) {
                    if(it.type == Material.BARRIER) it.type = Material.AIR
                }
            }

            alivePlayers.filter { !it.isOnline }.forEach {
                playerKilled(it, null)
            }
        }
    }

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    override suspend fun gameLoad() {
        repeat(rounds) {
            val kits: ArrayList<BrawlKit> = ArrayList()
            val unchosenPool: ArrayList<BrawlKit> = ArrayList(BrawlKit.entries)
            repeat(min(BrawlKit.entries.size, 4)) {
                val chosen = unchosenPool.random()
                unchosenPool.remove(chosen)
                kits.add(chosen)
            }

            roundKits.add(kits)
            roundPlacements.add(HashMap())

            loadMap(data.maps.random(), it)
        }
    }

    override suspend fun gamePregame() {
    }

    /**
     * The abstract method for spawning players in
     *
     * There was going to be some kind of system to do this automatically, but doing it manually seems to be a more flexible option, at least for now.
     *
     * @param cycle The stage where the players are spawned
     */
    override suspend fun spawn(cycle: SpawnCycle) {
        if(cycle != SpawnCycle.PRE_ROUND) return

        val currentMap = loadedMaps[roundIndex]
        val teamSpawns: HashMap<Team, BrawlSpawn> = HashMap()
        Team.entries.filter { it.playingTeam }.forEach {
            teamSpawns[it] = BrawlSpawn.entries.filter { entry ->
                entry.name.contains("SET_") && !teamSpawns.containsValue(entry)
            }.random()
        }

        suspendSync {
            teamSpawns.forEach {
                spawnPlayers(currentMap, it.key.getOnlinePlayers(), it.value)
            }
        }

        val spectators = Team.entries
            .filter { !it.playingTeam }
            .fold(arrayListOf<Player>()) { acc, entry -> acc.apply { addAll(entry.getOnlinePlayers()) } }
        suspendSync {
            spawnPlayers(currentMap, spectators, BrawlSpawn.SPECTATORS)
        }
    }

    fun selectKit(player: TumblingPlayer, brawlKit: BrawlKit, preRound: Boolean = false) {
        if(brawlKit !in roundKits[roundIndex])
            throw GameControllerException("Attempted to select a kit not included in the current round")

        playerKits[player] = brawlKit

        if(player.isOnline) {
            giveKit(player.bukkitPlayer!!)
            if(preRound) player.bukkitPlayer!!.inventory.addItem(kitSelector)
        }
    }

    fun giveKit(player: Player) {
        val brawlKit = playerKits[player.tumblingPlayer]
            ?: throw GameControllerException("Attempted to give kit items to a player without a selected kit")

        player.giveKit(brawlKit.kit)
    }

    fun playerKilled(player: TumblingPlayer, killer: Player?) {
        roundPlacements[roundIndex][player] = alivePlayers.size
        alivePlayers.remove(player)

        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            val message =
                if(killer != null) Format.formatKillMessage(killer.tumblingPlayer, player, it, getScoreSource(CommonScoreSource.KILL))
                else Format.formatDeathMessage(
                    player,
                    it,
                    false,
                    lastDamage = player.bukkitPlayer?.lastDamageCause?.cause ?: EntityDamageEvent.DamageCause.SUICIDE
                )

            it.sendMessage(message)
        }

        if(alivePlayers.size <= 1) {
            if(alivePlayers.size == 1) roundPlacements[roundIndex][player] = 1
            TreeTumblers.pluginScope.launch { endRound() }
        }
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

    @EventHandler
    fun brawlPlayerDeathEvent(event: PlayerDeathEvent) {
        val killed = event.player
        val killer = killed.killer

        // TODO: Remove all item drops except armor and util
        playerKilled(killed.tumblingPlayer, killer)
    }
}