package xyz.devcmb.tumblers.controllers.games.crumble

import com.sk89q.worldedit.WorldEdit
import com.sk89q.worldedit.bukkit.BukkitAdapter
import com.sk89q.worldedit.extent.clipboard.Clipboard
import com.sk89q.worldedit.math.BlockVector3
import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Color
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.Particle
import org.bukkit.command.CommandSender
import org.bukkit.entity.EntityType
import org.bukkit.entity.Player
import org.bukkit.entity.TNTPrimed
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.HandlerList
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.block.BlockPlaceEvent
import org.bukkit.event.entity.EntityDamageByEntityEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerMoveEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.LeatherArmorMeta
import org.bukkit.persistence.PersistentDataType
import org.bukkit.potion.PotionEffectType
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.crumble.kits.*
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.DebugToolkit
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.ScoreSource
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.LoadedMap
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.MiscUtils.suspendSync
import xyz.devcmb.tumblers.util.item.AdvancedItemStack
import xyz.devcmb.tumblers.util.openHandledInventory
import xyz.devcmb.tumblers.util.runTaskTimer
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates
import java.util.UUID
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    votable = true,
    maps = setOf(
        Map("warfare")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.text("Welcome to Crumble", NamedTextColor.YELLOW)) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Component.text("Cutscene step #2", NamedTextColor.GRAY)) { map ->
            teleport(0.0,128.0,0.0,0f,0f)
            delay(2000)
        }
    ),
    flags = setOf(),
    scores = hashMapOf(
        ScoreSource.KILL to 45,
        ScoreSource.ROUND_WIN to 100,
        ScoreSource.ROUND_LOSE to 10,
        ScoreSource.GAME_WIN to 250,
        ScoreSource.GAME_LOSE to 30
    )
) {
    companion object {
        @field:Configurable("games.crumble.max_kit_players")
        var maxPlayersPerKit: Int = 2

        @field:Configurable("games.crumble.tnt_detonation_time")
        var tntDetonationTime: Int = 80
    }

    val rounds = run { Team.entries.filter { it.playingTeam }.size - 1 }
    var currentRound = 1

    val currentMap: LoadedMap
        get() {
            return loadedMaps[currentRound - 1]
        }

    var roundActive = false

    var preRoundFreeze = false

    val matchups: ArrayList<MutableList<Pair<Team, Team>>> = ArrayList()
    val alivePlayers: HashMap<Team, ArrayList<Player>> = HashMap()
    val matchResults: ArrayList<HashMap<Team, RoundResult>> = ArrayList()

    val registeredKits: HashMap<String, Class<out Kit>> = HashMap()
    val kitTemplates: HashMap<String, Kit> = HashMap()
    val playerKits: HashMap<Player, Kit> = HashMap()
    val abilitiesUsed: ArrayList<Player> = ArrayList()

    val kitItems: ArrayList<ItemStack> = ArrayList()
    val actionBarTasks: ArrayList<BukkitRunnable> = ArrayList()

    val kitSelector: ItemStack = AdvancedItemStack(Material.COMPASS) {
        name(Component.text("Kit Selector", NamedTextColor.YELLOW))
        rightClick { player ->
            player.openHandledInventory("crumbleKitSelector")
        }
    }.build()

    val font = NamespacedKey("tumbling", "games/crumble")
    val killModel = NamespacedKey("tumbling", "crumble/kill")

    override val debugToolkit = object : DebugToolkit() {
        override val events: HashMap<String, (sender: CommandSender) -> Unit> = hashMapOf(
            "selector" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                sender.inventory.addItem(kitSelector.clone())
                sender.sendMessage(Format.success("Gave the kit selector successfully!"))
            },
            "setup_warfare" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                val worldEdit = WorldEdit.getInstance()
                val sessionManager = worldEdit.sessionManager

                val playerSession = sessionManager.get(BukkitAdapter.adapt(sender))
                var clipboard: Clipboard
                try {
                    clipboard = playerSession.clipboard.clipboards.lastOrNull()
                        ?: throw IllegalStateException("No clipboard loaded for this session")
                } catch(e: Exception) {
                    sender.sendMessage(Format.error("Your worldedit clipboard is empty!"))
                    return@to
                }

                if(clipboard.region.volume == 0L) {
                    sender.sendMessage(Format.error("Your worldedit clipboard is empty!"))
                    return@to
                }

                val positions: ArrayList<BlockVector3> = arrayListOf(
                    BlockVector3.at(0, 86, 0),
                    BlockVector3.at(-500, 86, 500),
                    BlockVector3.at(500, 86, -500),
                    BlockVector3.at(-500, 86, -500),
                    BlockVector3.at(500, 86, 500),
                    BlockVector3.at(0, 86, 500),
                    BlockVector3.at(500, 86, 0)
                )

                positions.forEach {
                    clipboard.paste(BukkitAdapter.adapt(sender.world), it)
                    sender.sendMessage(Format.success("Pasted at ${it.x()}, ${it.y()}, ${it.z()} successfully!"))
                }
            },
            "kit_kill" to { sender ->
                if(sender !is Player) {
                    sender.sendMessage(Format.error("Only players can trigger this event!"))
                    return@to
                }

                val kit = playerKits[sender]
                if(kit == null) {
                    sender.sendMessage(Format.error("You do not have a kit selected!"))
                    return@to
                }

                // maybe change the `killed` field to be optional
                kit.onKill(sender)
                sender.sendMessage(Format.success("Kill event sent successfully!"))
            }
        )

        override fun killEvent(killer: Player?, killed: Player?) = playerKillAnnouncement(killer, killed)
        override fun deathEvent(killed: Player?) = playerDeathAnnouncement(killed)
    }

    override suspend fun gameLoad() {
        registerKits()

        val teams = Team.entries.filter { it.playingTeam }.toMutableList()
        teams.forEach {
            alivePlayers.put(it, arrayListOf())
        }

        // ChatGPT code because idfk how to do any of this
        repeat(rounds) {
            val roundMatches = mutableListOf<Pair<Team, Team>>()

            for (i in 0 until teams.size / 2) {
                val a = teams[i]
                val b = teams[teams.lastIndex - i]
                roundMatches.add(a to b)
            }

            matchups.add(roundMatches)
            matchResults.add(hashMapOf())

            val last = teams.removeLast()
            teams.add(1, last)
        }

        for(i in 1..rounds) {
            val map = maps.random()
            loadMap(map, i)
        }
    }

    fun registerKits() {
        registerKit("archer", ArcherKit::class.java)
        registerKit("bomber", BomberKit::class.java)
        registerKit("fisher", FisherKit::class.java)
        registerKit("hunter", HunterKit::class.java)
        registerKit("ninja", NinjaKit::class.java)
        registerKit("sorcerer", SorcererKit::class.java)
        registerKit("warrior", WarriorKit::class.java)
        registerKit("worker", WorkerKit::class.java)
    }

    fun registerKit(id: String, kit: Class<out Kit>) {
        kitTemplates.put(id, kit.getConstructor(Player::class.java, CrumbleController::class.java).newInstance(null, this))
        registeredKits.put(id, kit)
    }

    override suspend fun spawn(cycle: SpawnCycle) {
        when(cycle) {
            SpawnCycle.PREGAME -> {
                val currentMap = loadedMaps.getOrNull(0)
                if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

                val pregameSpawn = currentMap.data.getList("spawns.pregame")
                    ?: throw GameControllerException("Pregame spawn not specified for ${currentMap.id}")

                val location: List<Double> = pregameSpawn.map {
                    if(it !is Double) throw GameControllerException("Teleport list does not contain exclusively doubles")
                    it
                }

                suspendSync {
                    gamePlayers.forEach {
                        it.teleport(location.unpackCoordinates(currentMap.world))
                        DebugUtil.info("Spawned player ${it.name} at $location")
                    }
                }
            }
            SpawnCycle.PRE_ROUND -> {
                val currentMap = loadedMaps.getOrNull(currentRound - 1)
                if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

                val currentMatchups = matchups[currentRound - 1]
                val spawnSetKeys = (1..7).map {
                    "spawns.ingame.arena$it"
                }.toMutableList()

                currentMatchups.forEachIndexed { index, matchup ->
                    val spawns: List<List<List<Double>>> = currentMap.data
                        .getList(spawnSetKeys.getOrNull(index) ?: spawnSetKeys.first())
                        ?.map { l1 ->
                            if(l1 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                            l1.map { l2 ->
                                if(l2 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                                l2.map {
                                    if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
                                    it
                                }
                            }
                        } ?: throw GameControllerException("Spawn set not found")

                    var firstOccupiedSpawns = 0
                    var secondOccupiedSpawns = 0

                    suspendSync {
                        gamePlayers.forEach {
                            it.spigot().respawn()
                            it.fireTicks = 0
                            val tumblingPlayer = it.tumblingPlayer ?: return@forEach

                            when(tumblingPlayer.team) {
                                matchup.first -> {
                                    val firstSpawnSet = spawns[0]
                                    val playerSpawn = firstSpawnSet[firstOccupiedSpawns]
                                    val location = playerSpawn.unpackCoordinates(currentMap.world)

                                    it.teleport(location)
                                    DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                    firstOccupiedSpawns++
                                }
                                matchup.second -> {
                                    val secondSpawnSet = spawns[1]
                                    val playerSpawn = secondSpawnSet[secondOccupiedSpawns]
                                    val location = playerSpawn.unpackCoordinates(currentMap.world)

                                    it.teleport(location)
                                    DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                    secondOccupiedSpawns++
                                }
                                else -> return@forEach
                            }
                        }
                    }
                }
            }
        }
    }

    override suspend fun gamePregame() {
        gameParticipants.forEach {
            it.inventory.addItem(kitSelector.clone())
            val task = object : BukkitRunnable() {
                override fun run() {
                    var component = Component.empty()
                    val kit = playerKits.get(it)
                    if(kit != null) {
                        // a link to the research I did to get these numbers
                        // https://confused-animal-c90.notion.site/Minecraft-resource-pack-UI-3206aa5edc9980e9a296d96d9ec07142
                        val bgSize = 69.5
                        // very important: if these are not roundToInt, it could be offset (I found this out the hard way)
                        val bgOffset = (kit.kitDisplayTextLength+((bgSize - kit.kitDisplayTextLength)/2)).roundToInt()
                        val fullOffset = ((bgSize - kit.kitDisplayTextLength) / 2).roundToInt()

                        component = Component.empty()
                            .append(UserInterfaceUtility.negativeSpace(fullOffset))
                            .append(Component.text("\uEF00").font(font))
                            .append(UserInterfaceUtility.negativeSpace(bgOffset))
                            .append(Component.text(kit.kitIcon).font(font))
                            .append(Component.text(" ${kit.name}"))
                            .shadowColor(ShadowColor.shadowColor(0))
                    }

                    it.sendActionBar(component)
                }
            }
            runTaskTimer(0, 5, task)
            actionBarTasks.add(task)
        }

        // TODO: Add some kind of countdown instead that yields
        delay(20 * 1000)

        suspendSync {
            gameParticipants.forEach {
                if(!playerKits.containsKey(it)) {
                    selectKit(
                        it,
                        registeredKits.keys.filter { registeredKit ->
                            playerKits.filter { kit -> kit.value.id == registeredKit }.size < maxPlayersPerKit
                        }.random()
                    )
                }

                it.closeInventory()
                it.inventory.clear()
            }
        }
    }

    override suspend fun gameOn() {
        repeat(rounds) {
            spawn(SpawnCycle.PRE_ROUND)
            gameParticipants.forEach {
                val tumblingPlayer = it.tumblingPlayer!!
                alivePlayers[tumblingPlayer.team]!!.add(it)
            }
            suspendSync(this::giveKits)
            abilitiesUsed.clear()
            preRoundFreeze = true
            delay(1000)
            preRoundFreeze = false
            announceMatchup()
            delay(7000) // prep stage
            dropWalls()
            roundActive = true

            while(true) {
                val currentAlivePlayers = alivePlayers.values.sumOf { it.size }
                if(currentAlivePlayers == 0) break
                delay(200)
            }

            roundActive = false
            delay(2000)
            currentRound++
        }
    }

    fun announceMatchup() {
        val roundMatchup = matchups[currentRound - 1]
        roundMatchup.forEach { matchup ->
            val players = setOf(
                *matchup.first.getOnlinePlayers().toTypedArray(),
                *matchup.second.getOnlinePlayers().toTypedArray()
            )

            val title = Title.title(
                Component.text("Round $currentRound", NamedTextColor.YELLOW).decorate(TextDecoration.BOLD),
                Component.empty()
                    .append(matchup.first.FormattedName)
                    .append(Component.text(" vs ", NamedTextColor.WHITE))
                    .append(matchup.second.FormattedName),
                Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
            )

            players.forEach { player ->
                player.showTitle(title)
            }
        }
    }

    suspend fun dropWalls() {
        val currentMap = loadedMaps.getOrNull(currentRound - 1)
        if(currentMap == null) throw GameControllerException("Current map for round $currentRound was not found")

        val walls = currentMap.data.getList("walls")
            ?.map { wall ->
                if(wall !is List<*>) throw GameControllerException("Walls list is not a 2d list")
                wall.map {
                    if(it !is Int && it !is Double) throw GameControllerException("Walls list element does not contain exclusively integers or doubles.")
                    it.toDouble()
                }
            }
            ?: throw GameControllerException("Wall list not specified for ${currentMap.id}")

        walls.forEach {
            val start = it.slice(0..2).unpackCoordinates(currentMap.world)
            val end = it.slice(3..5).unpackCoordinates(currentMap.world)

            suspendSync {
                for(x in min(start.x, end.x).toInt()..max(start.x, end.x).toInt())
                for(y in min(start.y, end.y).toInt()..max(start.y, end.y).toInt())
                for(z in min(start.z, end.z).toInt()..max(start.z, end.z).toInt()) {
                    val location = Location(currentMap.world, x.toDouble(), y.toDouble(), z.toDouble())
                    currentMap.world.spawnParticle(
                        Particle.BLOCK,
                        location,
                        20,
                        0.0,
                        0.0,
                        0.0,
                        location.block.blockData
                    )
                    location.block.type = Material.AIR
                }
            }
        }
    }

    fun sendTeamMessage(player: Player?, message: (receiver: Player) -> Component) {
        if(player == null) {
            Bukkit.getOnlinePlayers().forEach {
                it.sendMessage(message(it))
            }
            return
        }

        val matchup = getCurrentMatchup(player)!!
        val (team1, team2) = matchup

        team1.getOnlinePlayers().forEach { it.sendMessage(message(it)) }
        team2.getOnlinePlayers().forEach { it.sendMessage(message(it)) }
    }

    fun getCurrentMatchup(player: Player) = getCurrentMatchup(player.tumblingPlayer!!.team)

    fun getCurrentMatchup(team: Team): Pair<Team, Team>? {
        val roundMatchup = matchups[currentRound - 1]
        return roundMatchup.find { it.first == team || it.second == team }
    }

    @EventHandler
    fun playerScoreEvent(event: PlayerDeathEvent) {
        val killed = event.player
        val killer = killed.killer

        val tumblingPlayer = killed.tumblingPlayer!!
        val yourTeam = tumblingPlayer.team
        if(!yourTeam.playingTeam) return

        alivePlayers[tumblingPlayer.team]!!.remove(killed)
        val currentPlayerMatchup = getCurrentMatchup(killed)!!
        val enemyTeam =
            if(currentPlayerMatchup.first == yourTeam) currentPlayerMatchup.second
            else currentPlayerMatchup.first

        // this should only really happen if they die same-tick (or if there's only one person), but im not even sure if that'd be the case
        if(alivePlayers[enemyTeam]!!.isEmpty()) {
            roundDraw(yourTeam)
            roundDraw(enemyTeam)
        } else {
            alivePlayers[enemyTeam]!!.clear()
            roundLoss(yourTeam)
            roundWin(enemyTeam)
        }

        if(killer == null) {
            playerDeathAnnouncement(killed)
            return
        }

        event.showDeathMessages = false
        playerKillAnnouncement(killer, killed)
    }

    fun roundWin(team: Team) {
        val title = Title.title(
            Component.text("Round Won", NamedTextColor.GREEN),
            Component.text("Well played!", NamedTextColor.WHITE),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        matchResults[currentRound].put(team, RoundResult.WIN)
    }

    fun roundLoss(team: Team) {
        val title = Title.title(
            Component.text("Round Lost", NamedTextColor.RED),
            Component.text("Better luck next time!", NamedTextColor.WHITE),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        matchResults[currentRound].put(team, RoundResult.LOSS)
    }

    fun roundDraw(team: Team) {
        val title = Title.title(
            Component.text("Round Drawn", NamedTextColor.YELLOW),
            Component.empty(),
            Title.Times.times(Tick.of(3), Tick.of(60), Tick.of(3))
        )

        team.getOnlinePlayers().forEach {
            it.showTitle(title)
        }

        matchResults[currentRound].put(team, RoundResult.DRAW)
    }

    fun playerKillAnnouncement(killer: Player?, killed: Player?) {
        sendTeamMessage(killed) {
            Format.formatKillMessage(killer, killed, it, getScoreSource(ScoreSource.KILL))
        }

        if(killer != null) {
            grantScore(killer, ScoreSource.KILL)
        }
    }

    fun playerDeathAnnouncement(killed: Player?) {
        sendTeamMessage(killed) {
            Format.formatDeathMessage(killed, it)
        }

        // Natural death in this game does not give score
    }

    // Some maps have the spawn point right next to lava, so if you move immediately after spawning you'd just run right in
    @EventHandler
    fun playerMoveEvent(event: PlayerMoveEvent) {
        if(
            preRoundFreeze
            && (event.to.x != event.from.x || event.to.y != event.from.y || event.to.z != event.from.z)
        ) {
            event.isCancelled = true
        }
    }

    fun giveKits() = playerKits.keys.forEach(this::givePlayerKit)

    fun givePlayerKit(player: Player, pregame: Boolean = false) {
        val kit = playerKits[player]!!
        kit.cleanup()
        player.inventory.clear()

        kit.items.forEach {
            val item = it.clone()
            kitItems.add(item)

            if(MiscUtils.isArmor(item)) {
                if(item.type.name.contains("LEATHER")) {
                    item.itemMeta = item.itemMeta.also { meta ->
                        val meta = meta as LeatherArmorMeta
                        val playerTeam = player.tumblingPlayer!!.team
                        meta.setColor(Color.fromRGB(playerTeam.color.value()))
                    }
                }

                player.inventory.setItem(item.type.equipmentSlot, item)
            } else {
                player.inventory.addItem(item)
            }
        }

        val abilityItem = AdvancedItemStack(Material.PAPER) {
            name(Component.text("${kit.name} Ability: ${kit.abilityName}", NamedTextColor.AQUA))
            lore(
                MiscUtils.wrapComponent(
                    Component.text(kit.abilityDescription, NamedTextColor.WHITE),
                    40
                ).toTypedArray().map { it.decoration(TextDecoration.ITALIC, false) }
            )
            model(kit.inventoryModel)

            rightClick {
                useAbility(it)
            }
        }.build()

        val killItem = ItemStack(Material.PAPER).apply {
            itemMeta = itemMeta.also { meta ->
                meta.itemName(Component.text("Kill power: ${kit.killPowerName}", NamedTextColor.YELLOW))
                meta.itemModel = killModel
                meta.lore(
                    MiscUtils.wrapComponent(
                        Component.text(kit.killPowerDescription, NamedTextColor.WHITE),
                        40
                    ).toTypedArray().map { it.decoration(TextDecoration.ITALIC, false) }
                )
            }
        }

        // Make sure never to have over 7 items in a kit
        player.inventory.setItem(7, killItem)
        player.inventory.setItem(8, abilityItem)

        if(pregame) {
            player.inventory.addItem(kitSelector)
        }

        kitItems.add(killItem)
        kitItems.add(abilityItem)
    }

    fun selectKit(player: Player, id: String) {
        deselectKit(player)
        require(registeredKits.get(id) != null) { "Kit with id $id does not exist" }

        val kit = registeredKits[id]!!
            .getDeclaredConstructor(Player::class.java, CrumbleController::class.java)
            .newInstance(player, this)
        playerKits.put(player, kit)
        givePlayerKit(player, true)
        Bukkit.getServer().pluginManager.registerEvents(kit, TreeTumblers.plugin)
    }

    fun deselectKit(player: Player) {
        if(!playerKits.containsKey(player)) return
        HandlerList.unregisterAll(playerKits[player]!!)
        playerKits.remove(player)
    }

    fun useAbility(player: Player) {
        if(abilitiesUsed.contains(player)) {
            player.sendMessage(Format.error("You've already used your ability!"))
            return
        }

        if(!roundActive) {
            player.sendMessage(Format.error("You cannot use your ability until the round starts!"))
            return
        }

        playerKits[player]!!.onAbility()
        player.sendMessage(Format.success("Activated ability!"))
        abilitiesUsed.add(player)
    }

    @EventHandler
    fun playerDropItemEvent(event: PlayerDropItemEvent) {
        if(kitItems.find { it.isSimilar(event.itemDrop.itemStack) } != null)
            event.isCancelled = true
    }

    @EventHandler
    fun playerTntEvent(event: BlockPlaceEvent) {
        val player = event.player
        val block = event.block
        if(block.type != Material.TNT) return

        val item = event.itemInHand

        val location = block.location
        location.block.type = Material.AIR

        val tnt = location.world.spawnEntity(location, EntityType.TNT) as TNTPrimed
        tnt.persistentDataContainer.set(
            NamespacedKey("tumbling", "tnt_owner"),
            PersistentDataType.STRING,
            player.uniqueId.toString()
        )

        if(item.itemMeta?.persistentDataContainer?.get(BomberKit.nukeKey, PersistentDataType.BOOLEAN) == true) {
            // maybe un-hardcode this
            tnt.fuseTicks = BomberKit.nukeExplosionTicks
            tnt.persistentDataContainer.set(
                BomberKit.nukeKey,
                PersistentDataType.BOOLEAN,
                true
            )
        } else {
            tnt.fuseTicks = tntDetonationTime
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        if(event.entity !is Player) return
        if(!roundActive) event.isCancelled = true
    }

    @EventHandler(priority = EventPriority.HIGH)
    fun tntDamageEvent(event: EntityDamageEvent) {
        val player = event.entity
        if(player !is Player) return

        val causingEntity = event.damageSource.directEntity
        if(causingEntity == null || causingEntity !is TNTPrimed) return

        val dataContainer = causingEntity.persistentDataContainer
        val causingPlayerUUID = dataContainer.get(NamespacedKey("tumbling", "tnt_owner"), PersistentDataType.STRING)
        val causingPlayer = Bukkit.getPlayer(UUID.fromString(causingPlayerUUID))

        if(causingPlayerUUID == null || causingPlayer == null) {
            DebugUtil.severe("Could not find a causing player on an exploding tnt")
            event.isCancelled = true
            return
        }

        val causingTeam = causingPlayer.tumblingPlayer!!.team
        if(causingTeam.getOnlinePlayers().contains(player) && player != causingPlayer) {
            event.isCancelled = true
        }
    }

    @EventHandler
    fun playerHitEvent(event: EntityDamageByEntityEvent) {
        val damaged = event.entity
        val damager = event.damager

        if(damaged !is Player || damager !is Player) return

        if(damager.hasPotionEffect(PotionEffectType.BLINDNESS)) {
            damager.removePotionEffect(PotionEffectType.BLINDNESS)
        }
    }

    @EventHandler
    fun blockBreakEvent(event: BlockBreakEvent) {
        if(!roundActive) event.isCancelled = true
    }

    enum class RoundResult {
        WIN,
        LOSS,
        DRAW
    }
}