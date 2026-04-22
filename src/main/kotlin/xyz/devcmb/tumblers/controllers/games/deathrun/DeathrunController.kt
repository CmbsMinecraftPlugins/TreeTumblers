package xyz.devcmb.tumblers.controllers.games.deathrun

import com.destroystokyo.paper.event.server.ServerTickStartEvent
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import org.bukkit.*
import org.bukkit.attribute.Attribute
import org.bukkit.block.data.type.Gate
import org.bukkit.command.CommandSender
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.ArmorStand
import org.bukkit.entity.Fireball
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.entity.ProjectileHitEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ColorableArmorMeta
import org.bukkit.potion.PotionEffect
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.deathrun.traps.*
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.*
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import kotlin.collections.get
import kotlin.math.max

@EventGame
class DeathrunController : GameBase(
    id = "deathrun",
    name = "Deathrun",
    votable = true,
    maps = setOf(
        Map("forest")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Component.text("\uEA00").font(NamespacedKey("tumbling", "games/deathrun")))
            .append(Component.text(" Deathrun"))
        ) {
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Format.mm("In this game, 1 team at a time will be the <red>trappers</red>, while everyone else is a <aqua>runner</aqua>")) { map ->
            teleportConfig("cutscene.trapper_showcase")

            val armorStands: ArrayList<ArmorStand> = ArrayList()
            map.data.getList("cutscene.armor_stands")?.forEach {
                if(it !is List<*>) throw GameControllerException("Cutscene armor stand location table is not a list")
                val location = it.validateLocation(map.world) ?: throw GameControllerException("Cutscene armor stand locations are not valid")

                suspendSync {
                    map.world.spawn(location, ArmorStand::class.java) { stand ->
                        stand.equipment.setHelmet(ItemStack.of(Material.LEATHER_HELMET).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setBoots(ItemStack.of(Material.LEATHER_BOOTS).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        armorStands.add(stand)
                    }
                }

                delay(500)
            }

            delay(2500)
            suspendSync {
                armorStands.forEach(ArmorStand::remove)
                armorStands.clear()
            }
        },
        CutsceneStep(Format.mm("<red>Trappers</red> can activate traps that make progressing harder.<newline><red>Trappers</red> get points when they <yellow>damage</yellow> and when they <red>kill</red> players.<newline><aqua>Runners</aqua> have $lives lives, losing 1 whenever they take damage.")) {
            teleportConfig("cutscene.trap_showcase")

            val trap = (game as DeathrunController).mapTraps[0]!![0]
            delay(1000)
            trap.activate()
            delay(700)
        },
        CutsceneStep(Format.mm("The amount of score <aqua>runners</aqua> get from completing a run is based on their <yellow>placement</yellow><newline>The faster they complete the course, the more <yellow>score</yellow> they'll get")) {
            val game = game as DeathrunController
            suspendSync {
                game.summonScoreDisplay()
            }
            teleportConfig("cutscene.runner_score_showcase")
            delay(6000)
            suspendSync {
                game.endDisplay?.remove()
                game.endDisplay = null
            }
        },
        CutsceneStep(Format.mm("<b><green>Good Luck, Have Fun!</green></b>")) {}
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP,
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION,
        Flag.USE_SPECTATOR_DEATH_SYSTEM
    ),
    scores = hashMapOf(
        // this * placement = awarded score
        DeathrunScoreSource.RUN_COMPLETE to 8,
        // constant value
        DeathrunScoreSource.RUN_FAILED to 15,

        // split across the whole team
        DeathrunScoreSource.TRAP_KILL to 40,
        DeathrunScoreSource.TRAP_DAMAGE to 20
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    scoreboard = "deathrunScoreboard"
) {
    companion object {
        val font = NamespacedKey("tumbling", "games/deathrun")

        @field:Configurable("games.deathrun.lives")
        var lives: Int = 3
    }

    val playingTeams = Team.entries.filter { it.playingTeam }
    val rounds = playingTeams.size
    var currentRound = 0
    val roundIndex
        get() = max(currentRound - 1, 0)
    var roundActive = false
    var preRound = false

    val currentMap: LoadedMap
        get() {
            return loadedMaps[roundIndex]
        }

    val currentTeam: Team
        get() {
            return playingTeams[roundIndex]
        }

    val attackingPlayers: Set<Player>
        get() {
            return gameParticipants.filter { it.tumblingPlayer.team == currentTeam }.toSet()
        }

    val runningPlayers: Set<Player>
        get() {
            return gameParticipants.filter { it.tumblingPlayer.team != currentTeam }.toSet()
        }

    override val scoreMessages: HashMap<ScoreSource, (Int) -> Component> = hashMapOf(
        DeathrunScoreSource.RUN_COMPLETE to { amount ->
            gameMessage(
                Component.text("Run complete! ")
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        },
        DeathrunScoreSource.RUN_FAILED to { amount ->
            gameMessage(
                Component.text("Run failed! ")
                    .append(Component.text("[+$amount]", NamedTextColor.GOLD))
            )
        }
    )

    override val debugToolkit: DebugToolkit? = object : DebugToolkit() {
        override val events: HashMap<String, (CommandSender) -> Unit> = hashMapOf()

        override fun killEvent(killer: Player?, killed: Player?) {
            killed?.damage(1.0)
        }

        override fun deathEvent(killed: Player?) {
            killed?.damage(1.0)
        }
    }

    val traps: ArrayList<Class<out Trap>> = ArrayList()
    val mapTraps: HashMap<Int, ArrayList<Trap>> = HashMap()
    val mapCheckpoints: ArrayList<Triple<Location, Location, Location>> = ArrayList()
    val currentTraps: HashMap<Player, Int> = HashMap()
    val playerCheckpoints: HashMap<Player, Int> = HashMap()
    val cooldownTimes: HashMap<Int, Long> = HashMap()

    val alivePlayers: MutableSet<Player> = HashSet()

    val placements: ArrayList<HashMap<Player, Int>> = ArrayList()

    var ticksElapsed: Int = 0
    val completionTimes: HashMap<Player, Int> = HashMap()
    lateinit var timerActionBarTask: BukkitRunnable

    var endDisplay: TextDisplay? = null
    var endDisplayUpdateTask: BukkitRunnable? = null

    /**
     * The load sequence that each individual game should do
     *
     * This method is responsible for setting up maps, team arrangements, etc
     *
     * Anything that needs to be executed before players can access the game should be done here
     */
    @Suppress("UNCHECKED_CAST")
    override suspend fun gameLoad() {
        traps.add(MagmaFallTrap::class.java)
        traps.add(BeamRunTrap::class.java)
        traps.add(HappyGhastTrap::class.java)

        endDisplayUpdateTask = object : BukkitRunnable() {
            override fun run() {
                endDisplay?.text(Format.mm("<yellow>+${getRunCompletionScore()} Score</yellow>"))
            }
        }
        endDisplayUpdateTask!!.runTaskTimer(TreeTumblers.plugin, 0, 10)

        repeat(rounds) {
            placements.add(hashMapOf())

            val map = loadMap(maps.random(), it + 1)
            val roundMapTraps: ArrayList<Trap> = ArrayList(map.data.getList("traps")?.map { trap ->
                if(
                    trap !is HashMap<*,*>
                    || trap["id"] == null
                    || trap["id"] !is String
                    || trap["start"] !is List<*>
                    || trap["end"] !is List<*>
                    || (trap["start"] as List<*>).find { element -> element !is Int } != null
                    || (trap["end"] as List<*>).find { element -> element !is Int } != null
                    || trap["data"] == null
                    || trap["data"] !is HashMap<*,*>
                ) throw GameControllerException("Trap definition is not formatted correctly")

                val id = trap["id"]
                val trapClass = traps.find { clazz ->
                    val ins = clazz.getDeclaredConstructor(
                        DeathrunController::class.java,
                        ConfigurationSection::class.java,
                        Location::class.java,
                        Location::class.java
                    ).newInstance(
                        this,
                        YamlConfiguration().createSection("blah.blah.blah"),
                        Location(currentMap.world,0.0,0.0,0.0),
                        Location(currentMap.world,0.0,0.0,0.0)
                    )
                    
                    ins.id == id
                }?.getDeclaredConstructor(
                    DeathrunController::class.java,
                    ConfigurationSection::class.java,
                    Location::class.java,
                    Location::class.java
                ) ?: throw GameControllerException("Trap $id not found")

                // I tried doing `map.data.getConfigurationSection("traps.$index.data")` but that just always returned null no matter what
                // So this copilot fix is the best I've got :sob:
                // this took too long
                val data = YamlConfiguration().createSection("data", trap["data"] as HashMap<*,*>)

                val start = (trap["start"] as List<Int>).validateLocation(map.world)
                val end = (trap["end"] as List<Int>).validateLocation(map.world)
                trapClass.newInstance(this, data, start, end)
            } ?: throw GameControllerException("Traps list not found"))

            mapTraps.put(it, roundMapTraps)
        }
    }

    override suspend fun gamePregame() {
        val runnable = object : BukkitRunnable() {
            override fun run() {
                gameParticipants.forEach {
                    val time = completionTimes.getOrElse(it) { ticksElapsed }
                    val text = MiscUtils.formatMsTime(time * 50L)

                    it.sendActionBar(UserInterfaceUtility.backgroundTextCenter(
                        Component.text("\uEF00").font(font).shadowColor(ShadowColor.shadowColor(0)),
                        Component.text(text),
                        text,
                        69.5
                    ))
                }
            }
        }
        runnable.runTaskTimer(TreeTumblers.plugin, 0, 1)
        timerActionBarTask = runnable
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

        suspendSync {
            gamePlayers.forEach {
                if(it.tumblingPlayer.team == currentTeam) {
                    spawnAttacker(it)
                } else {
                    spawnMain(it)
                }
            }
        }
    }

    fun spawnMain(player: Player) {
        val mainSpawn: Location = currentMap.data.getList("spawns.main")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Main spawn set not found")

        player.teleport(mainSpawn)
    }

    fun spawnAttacker(player: Player) {
        val attackerSpawn: Location = currentMap.data.getList("spawns.attacker")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Spawn set not found")

        player.teleport(attackerSpawn)
        player.enableBossBar("deathrunCooldownBossbar")

        placements[roundIndex].put(player, -2)
        giveTrapItem(player, player.location)
        player.addPotionEffect(PotionEffect(PotionEffectType.SPEED, PotionEffect.INFINITE_DURATION, 1))
    }

    fun respawnRunner(player: Player) {
        player.fireTicks = 0

        val checkpoint = playerCheckpoints[player]
        if(checkpoint == null) {
            spawnMain(player)
        } else {
            player.teleport(mapCheckpoints[checkpoint].third)
        }
    }

    var roundEnded = false
    /**
     * The method for the main gameplay loop for an individual game
     *
     * This should contain any kind of game-specific logic, and round handling if applicable
     */
    override suspend fun gameOn() {
        Bukkit.getOnlinePlayers().forEach {
            it.enableBossBar("countdownBossbar")
        }

        repeat(rounds) {
            currentRound++
            cooldownTimes.clear()
            currentTraps.clear()
            spawn(SpawnCycle.PRE_ROUND)
            asyncCountdown(10, "deathrun_pregame_countdown") {}
            preRound()
            roundActive = true
            roundStart()
            asyncCountdown(120, "deathrun_game_countdown") { early ->
                if(!early) roundEnded = true
            }

            while(!roundEnded) {
                delay(500)
            }

            roundActive = false
            roundEnd()
            postRound()
        }
    }

    suspend fun preRound() {
        preRound = true
        alivePlayers.addAll(runningPlayers)
        alivePlayers.forEach {
            it.health = lives.toDouble() * 2
            it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = lives.toDouble() * 2
        }

        suspendSync {
            participatingSpectators.toList().forEach(this::unSpectate)
            summonScoreDisplay()
        }

        val checkpoints: List<Triple<Location, Location, Location>> = currentMap.data.getList("checkpoints")?.mapIndexed { index, checkpoint ->
            if(
                checkpoint !is HashMap<*,*>
                || checkpoint["start"] == null
                || checkpoint["end"] == null
                || checkpoint["respawn"] == null
                || checkpoint["start"] !is List<*>
                || checkpoint["end"] !is List<*>
                || checkpoint["respawn"] !is List<*>
            ) throw GameControllerException("Checkpoint definition not formatted correctly")

            val start = (checkpoint["start"] as List<*>)
                .validateLocation(currentMap.world)
                ?: throw GameControllerException("Checkpoint start location not provided or invalid")

            val end = (checkpoint["end"] as List<*>)
                .validateLocation(currentMap.world)
                ?: throw GameControllerException("Checkpoint end location not provided or invalid")

            val respawn = (checkpoint["respawn"] as List<*>)
                .validateLocation(currentMap.world)
                ?: throw GameControllerException("Checkpoint respawn location not provided or invalid")

            Triple(start, end, respawn)
        } ?: throw GameControllerException("Checkpoints list not found in map config")
        mapCheckpoints.addAll(checkpoints)

        delay(1000)
        val audience = Audience.audience(gamePlayers)
        audience.sendMessage(gameMessage(
            Format.mm("Round $currentRound: <team> are up!", Placeholder.component("team", currentTeam.formattedName))
        ))

        val title = Title.title(
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            Format.mm("<team> are up!", Placeholder.component("team", currentTeam.formattedName)),
            Title.Times.times(Tick.of(5), Tick.of(80), Tick.of(5))
        )
        audience.showTitle(title)


        delay(4000)

        while(currentTimer?.paused == true) {
            delay(1000)
        }

        suspendSync {
            gameParticipants.forEach { plr ->
                if(plr.tumblingPlayer.team == currentTeam) return@forEach
                gameParticipants.forEach { other ->
                    if(other == plr || !alivePlayers.contains(other)) return@forEach
                    plr.hidePlayer(TreeTumblers.plugin, other)
                }
            }
        }

        MiscUtils.subtitleCountdown(
            audience,
            Format.mm("<bold><yellow>Round $currentRound</yellow></bold>"),
            5
        )

        while(currentTimer?.paused == true) {
            delay(1000)
        }
        preRound = false
    }

    suspend fun postRound() {
        suspendSync {
            gameParticipants.forEach { plr ->
                plr.disableBossBar("deathrunCooldownBossbar")
                gameParticipants.forEach { other ->
                    if(other == plr || !gameParticipants.contains(other)) return@forEach
                    plr.showPlayer(TreeTumblers.plugin, other)
                }
            }
        }

        alivePlayers.clear()
        completionTimes.clear()
        mapCheckpoints.clear()
        playerCheckpoints.clear()

        ticksElapsed = 0

        suspendSync {
            endDisplay!!.remove()
            endDisplay = null

            gameParticipants.forEach {
                it.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
                it.heal(20.0)
            }

            attackingPlayers.forEach {
                it.removePotionEffect(PotionEffectType.SPEED)
                it.inventory.clear()
            }
        }
    }

    /**
     * The method to invoke after the game has ended
     */
    override suspend fun postGame() {
        timerActionBarTask.cancel()

        endDisplayUpdateTask?.cancel()
        endDisplayUpdateTask = null

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
        suspendSync {
            Bukkit.getOnlinePlayers().forEach {
                it.disableBossBar("countdownBossbar")
            }
        }
        super.cleanup()
    }

    /**
     * The method that gets called when a player joins the game during the [State.GAME_ON] state
     */
    override fun playerJoin(player: Player) {
        player.enableBossBar("countdownBossbar")
        if(player.tumblingPlayer.team == currentTeam) {
            spawnAttacker(player)
        } else {
            if(roundActive || preRound) {
                spawnMain(player)
            }

            if(roundActive && player.tumblingPlayer.team.playingTeam) {
                makeSpectator(player, false)
                player.sendMessage(Format.warning("You've joined while the round is active and have been placed into spectator. You will be put into the game next round."))
            } else if(preRound) {
                alivePlayers.add(player)
            }
        }
    }

    /**
     * The method that gets called when a player leaves the game during the [State.GAME_ON] state
     */
    override fun playerLeave(player: Player) {
        if(player.tumblingPlayer.team == currentTeam || !player.tumblingPlayer.team.playingTeam) return
        if(!preRound) {
            failRun(player)
        }
    }

    suspend fun roundStart() {
        val gateStart: Location = currentMap.data.getList("gate_start")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate start not found")

        val gateEnd: Location = currentMap.data.getList("gate_end")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Gate end not found")


        suspendSync {
            alivePlayers.forEach {
                if(!it.isOnline) {
                    failRun(it)
                }
            }
            gateStart.forEachRegion(gateEnd) {
                if(it.blockData !is Gate) return@forEachRegion
                it.blockData = (it.blockData as Gate).also { gate ->
                    gate.isOpen = true
                }
            }
        }
    }

    fun giveTrapItem(player: Player, pos: Location) {
        val index = mapTraps[roundIndex]!!.indexOfFirst { pos.isInRegion(it.from, it.to) }
        if(index == -1) return

        val playerTrap = currentTraps[player]
        if(playerTrap == index) return

        currentTraps[player] = index
        player.inventory.clear()

        val trap = mapTraps[roundIndex]!![index]
        player.inventory.addItem(AdvancedItemStack(Material.PAPER) {
            name(trap.name)
            droppable(false)
            model(trap.itemKey)

            rightClick {
                if(player.tumblingPlayer.team != currentTeam) return@rightClick

                if(!roundActive) {
                    player.sendMessage(Format.error("You can't use traps unless the round is active!"))
                    return@rightClick
                }

                if(cooldownTimes.contains(index)) {
                    player.sendMessage(Format.error("This trap is currently on cooldown!"))
                    return@rightClick
                }

                cooldownTimes.put(index, System.currentTimeMillis())
                TreeTumblers.pluginScope.launch {
                    val round = currentRound
                    trap.activate()
                    delay((trap.cooldown * 1000).toLong())
                    if(round == currentRound) cooldownTimes.remove(index)
                }
            }
        }.build())
    }

    fun summonScoreDisplay() {
        val displayPos = currentMap.data.getList("point_display.pos")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Point display position not provided or not valid")

        val scale = currentMap.data.getDouble("point_display.scale")
            .toFloat()

        val yaw = currentMap.data.getDouble("point_display.yaw")
        displayPos.yaw = yaw.toFloat()

        displayPos.chunk.load()
        endDisplay = currentMap.world.spawn(displayPos, TextDisplay::class.java) {
            it.isPersistent = true
            it.lineWidth = 400
            it.alignment = TextDisplay.TextAlignment.CENTER
            it.text(Format.mm("<yellow>+<red>?</red> Score</yellow>"))
            it.transformation = Transformation(
                Vector3f(),
                AxisAngle4f(),
                Vector3f(scale, scale, scale),
                AxisAngle4f()
            )
        }
    }

    fun makePlayerSpectate(player: Player) {
        alivePlayers.remove(player)
        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
        makeSpectator(player, false)

        if(alivePlayers.isEmpty()) {
            roundEnded = true
        }
    }

    fun completeRun(player: Player) {
        makePlayerSpectate(player)
        val placement = placements[roundIndex].size + 1
        Audience.audience(Bukkit.getOnlinePlayers()).sendMessage(
            gameMessage(Format.mm(
                "<green><player> has completed the run <white>${placement}${MiscUtils.getOrdinalSuffix(placement)}</white> in <white>${MiscUtils.formatMsTime(ticksElapsed * 50L)}</white>!</green>",
                Placeholder.component("player", Format.formatPlayerName(player.tumblingPlayer))
            ))
        )

        grantScore(player, DeathrunScoreSource.RUN_COMPLETE, getRunCompletionScore())
        player.showTitle(Title.title(
            Component.empty(),
            Format.success("Run complete")
                .append(Component.text(" [+${getRunCompletionScore()}]", NamedTextColor.GOLD)),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))

        completionTimes.put(player, ticksElapsed)
        placements[roundIndex].put(player, completionTimes.size)

        MiscUtils.spawnFirework(player, FireworkEffect.builder()
            .trail(false)
            .flicker(false)
            .withColor(Color.YELLOW)
            .withColor(Color.ORANGE)
            .with(FireworkEffect.Type.BALL_LARGE)
            .build()
        )

    }

    fun getRunCompletionScore(): Int {
        return (
            Team.entries.filter { it.playingTeam && it != currentTeam }.sumOf { it.getAllPlayers().size }
                - placements[roundIndex].filter { it.value != -1 }.size
        ) * getScoreSource(DeathrunScoreSource.RUN_COMPLETE)
    }

    fun failRun(player: Player) {
        currentTeam.getOnlinePlayers().forEach {
            MiscUtils.announceKill(it, player, getScoreSource(DeathrunScoreSource.TRAP_KILL))
        }

        makePlayerSpectate(player)
        grantScore(player, DeathrunScoreSource.RUN_FAILED)
        Bukkit.broadcast(
            gameMessage(Format.mm(
                "<red><player> has been eliminated!</red>",
                Placeholder.component("player", Format.formatPlayerName(player.tumblingPlayer))
            ))
        )
        grantTeamScore(currentTeam, DeathrunScoreSource.TRAP_KILL)
        placements[roundIndex].put(player, -1)

        player.showTitle(Title.title(
            Component.empty(),
            Format.error("Run failed")
                .append(Component.text(" [+${getScoreSource(DeathrunScoreSource.RUN_FAILED)}]", NamedTextColor.GOLD)),
            Title.Times.times(Tick.of(5), Tick.of(40), Tick.of(5))
        ))
    }

    suspend fun roundEnd() {
        suspendSync {
            gameParticipants.forEach {
                if(!placements[roundIndex].containsKey(it)) {
                    failRun(it)
                }

                it.sendMessage(gameMessage(Component.text("Round Over!")))
                it.showTitle(Title.title(
                    Format.mm("<red><bold>Round Over</bold></red>"),
                    Component.empty(),
                    Title.Times.times(Tick.of(0), Tick.of(50), Tick.of(10))
                ))
            }

        }

        roundEnded = false
        cancelCountdown()
        delay(4000)
    }

    fun setCheckpoint(player: Player, index: Int) {
        val currentCheckpoint = playerCheckpoints[player] ?: -1
        if(index <= currentCheckpoint) return

        playerCheckpoints.put(player, index)
        player.showTitle(Title.title(
            Component.empty(),
            Format.success("Checkpoint!"),
            Title.Times.times(Tick.of(5), Tick.of(30), Tick.of(5))
        ))
    }

    @EventHandler
    fun attackerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(player.tumblingPlayer.team != currentTeam || currentState != State.GAME_ON) return

        val pos = event.to
        giveTrapItem(event.player, pos)
    }

    @EventHandler
    fun runnerMoveEvent(event: PlayerMoveEvent) {
        val player = event.player
        if(
            !roundActive
            || !alivePlayers.contains(player)
        ) return

        val pos = event.to
        val winStart = currentMap.data.getList("win_zone_start")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Win start location not found")

        val winEnd = currentMap.data.getList("win_zone_end")
            ?.validateLocation(currentMap.world)
            ?: throw GameControllerException("Win end location not found")

        if(pos.isInRegion(winStart, winEnd)) {
            completeRun(player)
        }
    }

    @EventHandler
    fun openGateEvent(event: PlayerInteractEvent) {
        val block = event.clickedBlock ?: return

        if(block.blockData is Gate) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(event.isCancelled) return

        val player = event.entity
        if(player !is Player) return

        if(!alivePlayers.contains(player)) {
            event.isCancelled = true
            return
        }

        Bukkit.getOnlinePlayers().forEach {
            it.sendMessage(
                Format.formatDeathMessage(
                    player,
                    it,
                    it.tumblingPlayer.team == currentTeam,
                    getScoreSource(DeathrunScoreSource.TRAP_DAMAGE)
                )
            )

        }
        grantTeamScore(currentTeam, DeathrunScoreSource.TRAP_DAMAGE)

        event.damage = 2.0
        respawnRunner(player)
    }

    @EventHandler
    fun playerRunFail(event: PlayerDeathEvent) {
        val player = event.player
        failRun(player)
        // no need to cancel because the spectator kill flag thing already does it
    }

    @EventHandler
    fun tickEvent(event: ServerTickStartEvent) {
        if(!roundActive) return
        ticksElapsed++
    }

    @EventHandler
    fun moveEvent(event: PlayerMoveEvent) {
        if(!roundActive) return

        val voidHeight = currentMap.data.getInt("void_height")
        if(event.to.y < voidHeight) {
            event.player.damage(1.0)
        }
    }

    @EventHandler
    fun checkpointEvent(event: PlayerMoveEvent) {
        val checkpoint = mapCheckpoints.indexOfFirst { event.to.isInRegion(it.first, it.second) }
        if(checkpoint == -1 || event.player !in alivePlayers) return

        setCheckpoint(event.player, checkpoint)
    }

    @EventHandler
    fun destructionlessFireballEvent(event: ProjectileHitEvent) {
        if(event.entity !is Fireball) return

        event.isCancelled = true
        event.entity.location.createExplosion(2.0f,false,false)
        event.entity.remove()
    }

    class DeathrunTrapException(override val message: String) : Exception()

    enum class DeathrunScoreSource(override val id: String) : ScoreSource {
        RUN_COMPLETE("deathrun_run_complete"),
        RUN_FAILED("deathrun_run_failed"),
        TRAP_DAMAGE("deathrun_trap_damage"),
        TRAP_KILL("deathrun_trap_kill")
    }
}