package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.launch
import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.Particle
import org.bukkit.damage.DamageSource
import org.bukkit.damage.DamageType
import org.bukkit.entity.Player
import org.bukkit.entity.ThrownPotion
import org.bukkit.event.EventHandler
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.FoodLevelChangeEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileLaunchEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Vector
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.Timer
import xyz.devcmb.tumblers.engine.base.RoundedGame
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.events.UseAdvancedItemEvent
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.getRandomCirclePoint
import xyz.devcmb.tumblers.util.giveKit
import xyz.devcmb.tumblers.item.advanced.AdvancedItemStack
import xyz.devcmb.tumblers.util.disableBossBar
import xyz.devcmb.tumblers.util.enableActionBar
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateList
import xyz.devcmb.tumblers.util.validateLocation
import xyz.devcmb.tumblers.util.withY
import kotlin.math.min
import kotlin.math.pow
import kotlin.random.Random
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
        click {
            it.openHandledInventory("brawlKitSelector")
        }
    }.build()

    val roundKits: ArrayList<ArrayList<BrawlKit>> = ArrayList()
    val playerKits: HashMap<TumblingPlayer, BrawlKit> = HashMap()

    val alivePlayers: ArrayList<TumblingPlayer> = ArrayList()
    val aliveTeams: HashSet<Team>
        get() = HashSet(alivePlayers.map { it.team })
    val roundPlacements: ArrayList<HashMap<TumblingPlayer, Int>> = ArrayList()

    var borderRunnable: BukkitRunnable? = null

    override val scoreMessages: HashMap<ScoreSource, (score: Int) -> Component> = hashMapOf(
        BrawlScoreSource.SURVIVE_ONE_MINUTE to {
            gameMessage(Format.mm("Survived one minute <gold>[+$it]</gold>"))
        }
    )

    var kitSelectActive: Boolean = false
    override suspend fun preRound() {
        playerKits.clear()
        suspendSync {
            val spectators = Team.entries
                .filter { !it.playingTeam }
                .flatMap { it.getOnlinePlayers() }
            spectators.forEach { makeSpectator(it, participating = false) }

            participatingSpectators.toList().forEach(this::unSpectate)

            spawnPlayers(
                loadedMaps[roundIndex],
                gamePlayers.mapNotNull { it.bukkitPlayer },
                BrawlSpawn.PRE_ROUND
            )

            gameParticipants.mapNotNull { it.bukkitPlayer }.forEach {
                it.inventory.clear()
                it.gameMode = GameMode.ADVENTURE
                it.health = 20.0
                it.foodLevel = 20
                it.isFlying = false
                it.allowFlight = false
                it.inventory.addItem(kitSelector)
            }
        }

        kitSelectActive = true
        timer(Timer(45) {
            id = "brawl_kit_select"
            title = "Kit Select"
            joined = true
        })

        alivePlayers.clear()
        alivePlayers.addAll(gameParticipants)
        suspendSync {
            gameParticipants.mapNotNull { it.bukkitPlayer }.forEach {
                it.inventory.clear()
            }

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
                    plr.foodLevel = 20
                    plr.saturation = 3f
                    plr.closeInventory()
                }
            }
        }
        kitSelectActive = false

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
            alivePlayers.mapNotNull { it.bukkitPlayer }.forEach {
                it.gameMode = GameMode.SURVIVAL
            }
        }

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

            alivePlayers
                .filter { !it.isOnline }
                .sortedBy { it.team.priority }
                .toList()
                .forEach { playerKilled(it, null) }
        }

        currentTimer!!.timeExecution(4.minutes.inWholeSeconds.toInt()) {
            Bukkit.broadcast(gameMessage(Format.mm("<red>The border has started shrinking!</red>")))
            setupBorderTask()
        }

        currentTimer!!.intervalExecution(1.minutes.inWholeSeconds.toInt()) {
            alivePlayers.forEach { grantScore(it, BrawlScoreSource.SURVIVE_ONE_MINUTE) }
        }
    }

    var borderRadius: Int = 0
    fun setupBorderTask() {
        val currentMap = loadedMaps[roundIndex]
        borderRadius = currentMap.data.getInt("starting_border_radius")

        val mapOrigin = currentMap.data.getList("origin")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Current map does not have a valid origin")

        borderRunnable = object : BukkitRunnable() {
            override fun run() {
                borderRadius -= 1
                alivePlayers.mapNotNull { it.bukkitPlayer }.forEach {
                    if(it.location.withY(mapOrigin.y).distanceSquared(mapOrigin) > borderRadius.toDouble().pow(2.0)) {
                        it.damage(2.0)
                    }
                }

                repeat(borderRadius * 10) {
                    val center = mapOrigin.clone()
                    center.y += ((-5..5).random())

                    val point = getRandomCirclePoint(center, borderRadius.toDouble())
                    currentMap.world.spawnParticle(
                        Particle.DUST,
                        point.x,
                        point.y,
                        point.z,
                        3,
                        Particle.DustOptions(Color.RED, Random.nextDouble(1.0, 5.5).toFloat())
                    )
                }
            }
        }
        borderRunnable!!.runTaskTimer(TreeTumblers.plugin, 0, 20)
    }

    override suspend fun postRound() {
        alivePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            it.allowFlight = true
            it.isFlying = true
            it.velocity = it.velocity.add(Vector(0.0, 0.5, 0.0))
        }
        borderRunnable?.cancel()
        borderRunnable = null
        super.postRound()
    }

    override suspend fun postGame() {
        gameParticipants.forEach {
            it.disableBossBar("brawlActionBar")
        }
        super.postGame()
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

        gameParticipants.forEach {
            it.enableActionBar("brawlActionBar")
        }
    }

    override suspend fun gamePregame() {
    }

    val teamSpawns: HashMap<Team, BrawlSpawn> = HashMap()
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
        teamSpawns.clear()
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
            .flatMap { it.getOnlinePlayers() }
        suspendSync {
            spawnPlayers(currentMap, spectators, BrawlSpawn.SPECTATORS)
        }
    }

    fun selectKit(player: TumblingPlayer, brawlKit: BrawlKit, preRound: Boolean = false) {
        if(!kitSelectActive) return

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

        killer?.let { grantScore(it, CommonScoreSource.KILL) }
        alivePlayers.forEach { grantScore(it, CommonScoreSource.OUTLAST) }

        gamePlayers.mapNotNull { it.bukkitPlayer }.forEach {
            val message =
                if(killer != null) Format.formatKillMessage(
                    killer.tumblingPlayer,
                    player,
                    it,
                    getScoreSource(CommonScoreSource.KILL),
                    if(it != killer) getScoreSource(CommonScoreSource.OUTLAST) else null
                ) else Format.formatDeathMessage(
                    player,
                    it,
                    true,
                    score = getScoreSource(CommonScoreSource.OUTLAST),
                    lastDamage = player.bukkitPlayer?.lastDamageCause?.cause ?: EntityDamageEvent.DamageCause.SUICIDE
                )

            it.sendMessage(message)
        }

        if(!alivePlayers.any { it.team == player.team }) {
            Bukkit.broadcast(gameMessage(Format.mm("<red><team:${player.team.name.lowercase()}:name> has been eliminated!</red>")))
        }

        if(aliveTeams.size <= 1) {
            if(aliveTeams.size == 1) {
                alivePlayers.filter { it.team == aliveTeams.first() }.forEach {
                    roundPlacements[roundIndex][it] = -1
                }
            }
            TreeTumblers.pluginScope.launch { endRound() }
        }
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] and [State.PREGAME] states
     */
    override fun playerJoin(player: Player) {
        if(kitSelectActive) {
            spawnPlayers(loadedMaps[roundIndex], setOf(player), BrawlSpawn.PRE_ROUND)

            if(playerKits.containsKey(player.tumblingPlayer)) giveKit(player)
            player.inventory.addItem(kitSelector)
        } else if(preRound) {
            if(player.tumblingPlayer.team.playingTeam) {
                giveKit(player)
                spawnPlayers(loadedMaps[roundIndex], setOf(player), teamSpawns[player.tumblingPlayer.team]!!)
            } else {
                spawnPlayers(loadedMaps[roundIndex], setOf(player), BrawlSpawn.SPECTATORS)
            }
        } else {
            if(player.tumblingPlayer.team.playingTeam) {
                makeSpectator(player)
                player.sendMessage(Format.warning("You've joined while the round is active and have been placed into spectator. You will be put into the game next round."))
            }
            spawnPlayers(loadedMaps[roundIndex], setOf(player), BrawlSpawn.SPECTATORS)
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] and [State.PREGAME] state
     */
    override fun playerLeave(player: Player) {
        if(player.tumblingPlayer in alivePlayers) {
            playerKilled(player.tumblingPlayer, (player.lastDamageCause as? EntityDamageByEntityEvent)?.damager as? Player)
        }
    }

    @EventHandler
    fun brawlPlayerDeathEvent(event: PlayerDeathEvent) {
        if(!roundActive) return
        val killed = event.player
        val killer = killed.killer

        playerKilled(killed.tumblingPlayer, killer)
    }

    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        if(!roundActive) return
        if(event.to.y <= loadedMaps[roundIndex].data.getInt("void_height"))
            event.player.damage(100.0, DamageSource.builder(DamageType.OUT_OF_WORLD).build())
    }

    @EventHandler
    fun useAdvancedItemEvent(event: UseAdvancedItemEvent) {
        if(!roundActive && event.ctx.item.type != Material.COMPASS) event.isCancelled = true
    }

    @EventHandler
    fun playerThrowPotionEvent(event: ProjectileLaunchEvent) {
        if(event.entity is ThrownPotion && !roundActive) event.isCancelled = true
    }

    @EventHandler
    fun playerHungryEvent(event: FoodLevelChangeEvent) {
        if(!roundActive) event.isCancelled = true
    }

    @EventHandler
    fun blockPlaceEvent(event: BlockPlaceEvent) {
        if(!roundActive) return

        val buildLimit = loadedMaps[roundIndex].data.getInt("build_limit")
        if(event.block.location.y >= buildLimit) {
            event.isCancelled = true
            event.player.sendMessage(Format.error("You cannot build this high!"))
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(event.entity is Player && !roundActive) event.isCancelled = true
    }

    @EventHandler
    fun dropItemEvent(event: PlayerDropItemEvent) {
        if(roundActive) return
        event.isCancelled = true
    }

    @EventHandler
    fun playerInteractEvent(event: PlayerInteractEvent) {
        if(roundActive) return

        val item = event.player.inventory.itemInMainHand
        if(item.type.isEdible) {
            event.isCancelled = true
        }
    }

    enum class BrawlScoreSource(override val id: String) : ScoreSource {
        SURVIVE_ONE_MINUTE("brawl_survive_one_minute")
    }
}