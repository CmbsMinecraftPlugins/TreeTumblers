package xyz.devcmb.tumblers.controllers.games.crumble

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.format.TextDecoration
import net.kyori.adventure.title.Title
import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.controllers.games.crumble.kits.ArcherKit
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.ScoreSource
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
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
    }

    val rounds = run { Team.entries.filter { it.playingTeam }.size - 1 }
    var currentRound = 1
    val matchups: ArrayList<MutableList<Pair<Team, Team>>> = ArrayList()

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

    override suspend fun gameLoad() {
        registerKits()

        // ChatGPT code because idfk how to do any of this
        val teams = Team.entries.filter { it.playingTeam }.toMutableList()
        repeat(rounds) {
            val roundMatches = mutableListOf<Pair<Team, Team>>()

            for (i in 0 until teams.size / 2) {
                val a = teams[i]
                val b = teams[teams.lastIndex - i]
                roundMatches.add(a to b)
            }

            matchups.add(roundMatches)

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
    }

    fun registerKit(id: String, kit: Class<out Kit>) {
        kitTemplates.put(id, kit.getConstructor(Player::class.java).newInstance(null))
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
                            return@map l1.map { l2 ->
                                if(l2 !is List<*>) throw GameControllerException("Spawn set is not a 2d list")
                                return@map l2.map {
                                    if(it !is Double) throw GameControllerException("Spawn locations do not contain exclusively doubles")
                                    it
                                }
                            }
                        } ?: throw GameControllerException("Spawn set not found")

                    var firstOccupiedSpawns = 0
                    var secondOccupiedSpawns = 0

                    gamePlayers.forEach {
                        val tumblingPlayer = it.tumblingPlayer ?: return@forEach

                        when(tumblingPlayer.team) {
                            matchup.first -> {
                                val firstSpawnSet = spawns[0]
                                val playerSpawn = firstSpawnSet[firstOccupiedSpawns]
                                val location = playerSpawn.unpackCoordinates(currentMap.world)

                                suspendSync {
                                    it.teleport(location)
                                }
                                DebugUtil.info("Spawned ${it.name} at $playerSpawn")

                                firstOccupiedSpawns++
                            }
                            matchup.second -> {
                                val secondSpawnSet = spawns[1]
                                val playerSpawn = secondSpawnSet[secondOccupiedSpawns]
                                val location = playerSpawn.unpackCoordinates(currentMap.world)

                                suspendSync {
                                    it.teleport(location)
                                }
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
                        val bgOffset = (kit.kitDisplayTextLength+((bgSize - kit.kitDisplayTextLength)/2)).toInt()
                        val fullOffset = ((bgSize - kit.kitDisplayTextLength) / 2).toInt()

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
            runTaskTimer(task, 0, 5)
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
            giveKits()
            abilitiesUsed.clear()
            delay(1500)

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

            delay(7000) // prep stage
            // TODO: Drop walls
            currentRound++
        }
    }

    fun giveKits() {
        playerKits.forEach { player, kit ->
            player.inventory.clear()

            kit.items.forEach {
                val item = it.clone()
                kitItems.add(item)
                player.inventory.addItem(item)
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

            kitItems.add(killItem)
            kitItems.add(abilityItem)
        }
    }

    fun selectKit(player: Player, id: String) {
        deselectKit(player)
        require(registeredKits.get(id) != null) { "Kit with id $id does not exist" }

        val kit = registeredKits[id]!!
            .getDeclaredConstructor(Player::class.java)
            .newInstance(player)
        playerKits.put(player, kit)
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

        playerKits[player]!!.onAbility()
        abilitiesUsed.add(player)
    }

    @EventHandler
    fun playerDropItemEvent(event: PlayerDropItemEvent) {
        if(kitItems.find { it.isSimilar(event.itemDrop.itemStack) } != null)
            event.isCancelled = true
    }
}