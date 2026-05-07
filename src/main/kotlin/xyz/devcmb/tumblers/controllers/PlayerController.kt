package xyz.devcmb.tumblers.controllers

import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Location
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.block.BlockFace
import org.bukkit.damage.DamageType
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.block.BlockBreakEvent
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.TumblingGenericException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.ui.PlayerUIController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.forEachRegion
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.item.AdvancedItemRegistry
import xyz.devcmb.tumblers.util.runTask
import xyz.devcmb.tumblers.util.tp
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.validateLocation
import java.util.UUID

@Controller("playerController", Controller.Priority.MEDIUM)
class PlayerController : IController {
    val playerUIControllers: HashMap<Player, PlayerUIController> = HashMap()
    val hiddenPlayers: MutableSet<Player> = HashSet()
    lateinit var players: ArrayList<TumblingPlayer>
    var isChatMuted = false

    var currentNametagMode: NametagMode = NametagMode.ALL
        set(value) {
            field = value
            updateNametagVisibility()
        }

    val nameTags: HashMap<Player, TextDisplay> = HashMap()

    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    private val gameController: GameController by lazy {
        ControllerDelegate.getController<GameController>()
    }

    private val spectatorController: SpectatorController by lazy {
        ControllerDelegate.getController<SpectatorController>()
    }

    companion object {
        @field:Configurable("lobby.world")
        var lobbyWorld: String = "world"

        @field:Configurable("lobby.spawn.start")
        var lobbySpawnStart: List<Int> = listOf(-56, 190, 13)

        @field:Configurable("lobby.spawn.end")
        var lobbySpawnEnd: List<Int> = listOf(-80,190,3)

        @field:Configurable("lobby.spawn.yaw")
        var lobbySpawnYaw: Double = -90.0

        @field:Configurable("lobby.spawn.pitch")
        var lobbySpawnPitch: Double = 0.0

        @field:Configurable("lobby.spawn.floor")
        var lobbySpawnFloor: Material = Material.STONE_BRICKS
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            players = databaseController.getAllPlayerData()
        }
    }

    fun registerTumblingPlayer(uuid: UUID, name: String, team: Team, score: Int) {
        val player = TumblingPlayer(uuid)
        player.bukkitPlayer = Bukkit.getPlayer(uuid)
        player.name = name
        player.team = team
        player.score = score

        players.add(player)
    }

    fun unregisterTumblingPlayer(uuid: UUID) {
        players.removeIf { it.uuid == uuid }
    }

    fun setPlayerTeam(uuid: UUID, team: Team) {
        players.find { it.uuid == uuid }!!.team = team
    }

    fun spawnHub(player: Player) {
        player.tp(getLobbyPosition())
    }

    fun getLobbyPosition(): Location {
        val hub = Bukkit.getWorld(lobbyWorld)!!
        val startLocation = lobbySpawnStart.validateLocation(hub)
            ?: throw TumblingGenericException("Start location for hub spawning is not a valid location list")

        val endLocation = lobbySpawnEnd.validateLocation(hub)
            ?: throw TumblingGenericException("End location for hub spawning is not a valid location list")

        val validSpawns: ArrayList<Location> = ArrayList()
        startLocation.forEachRegion(endLocation) {
            if(it.type == lobbySpawnFloor && it.getRelative(BlockFace.UP).isEmpty) {
                validSpawns.add(it.location.clone().add(0.0,1.0,0.0))
            }
        }

        val location = validSpawns.random().clone().toCenterLocation()
        location.pitch = lobbySpawnPitch.toFloat()
        location.yaw = lobbySpawnYaw.toFloat()
        return location
    }

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.inventory.clear()
        player.gameMode = GameMode.ADVENTURE
        player.isFlying = false
        player.allowFlight = false
        player.clearActivePotionEffects()

        player.vehicle?.let {
            player.leaveVehicle()
            it.remove()
        }

        runTask {
            spawnHub(player)
            reloadNametag(player)
        }

        player.getAttribute(Attribute.MAX_HEALTH)?.baseValue = 20.0
        player.health = 20.0

        Bukkit.getOnlinePlayers().forEach {
            player.unlistPlayer(it)
            it.unlistPlayer(player)
        }

        hiddenPlayers.forEach {
            player.hidePlayer(TreeTumblers.plugin, it)
        }

        event.joinMessage(Component.empty())
        playerUIControllers.forEach { it.value.playerJoin(player) }
        playerUIControllers.put(player, PlayerUIController(player))

        if(Constants.IS_DEVELOPMENT) {
            DebugUtil.subscribe(player, DebugUtil.DebugLogLevel.WARNING)
            player.sendMessage(
                Component.text("Developer mode is active. You have automatically been subscribed to the warning debug channel.")
                    .color(NamedTextColor.YELLOW)
            )
        }

        val tumblingPlayer = players.find { it.uuid == player.uniqueId }!!
        tumblingPlayer.bukkitPlayer = player

        player.displayName(Format.formatPlayerName(tumblingPlayer))
        Bukkit.broadcast(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("+").color(NamedTextColor.GREEN))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(tumblingPlayer).color(NamedTextColor.WHITE))
        )
    }

    @EventHandler
    fun playerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val tumblingPlayer = player.tumblingPlayer

        hiddenPlayers.remove(player)

        playerUIControllers[player]?.cleanup()
        playerUIControllers.remove(player)
        playerUIControllers.forEach { it.value.playerLeave(player) }

        nameTags[player]?.remove()

        event.quitMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("-").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(tumblingPlayer).color(NamedTextColor.WHITE))
        )

        tumblingPlayer.bukkitPlayer = null
    }

    @EventHandler
    fun playerInteract(event: PlayerInteractEvent) {
        AdvancedItemRegistry.handleInteract(event)
    }

    @EventHandler
    fun playerDropItem(event: PlayerDropItemEvent) {
        AdvancedItemRegistry.handleDrop(event)
    }

    @EventHandler
    fun playerKillEvent(event: PlayerDeathEvent) {
        val killed = event.player
        val killer = killed.killer

        if(killer == null) return

        val currentGame = gameController.activeGame
        val score = currentGame?.getScoreSource(CommonScoreSource.KILL)

        MiscUtils.announceKill(killer, killed, if(score != null && score != 1) score else null)
    }

    @EventHandler(priority = EventPriority.LOWEST)
    fun playerFireworkDamageEvent(event: EntityDamageEvent) {
        if(event.entity is Player && event.damageSource.damageType == DamageType.FIREWORKS)
            event.isCancelled = true
    }


    @EventHandler
    fun playerMilkEvent(event: PlayerItemConsumeEvent) {
        if(event.item.type != Material.MILK_BUCKET) return

        event.isCancelled = true
        if(event.player.inventory.itemInMainHand.type == Material.MILK_BUCKET) {
            event.player.inventory.setItemInMainHand(ItemStack.of(Material.BUCKET))
        } else if (event.player.inventory.itemInOffHand.type == Material.MILK_BUCKET) {
            event.player.inventory.setItemInOffHand(ItemStack.of(Material.BUCKET))
        }
    }

    @EventHandler
    fun playerStatEvent(event: PlayerStatisticIncrementEvent) {
        event.isCancelled = true
    }

    /*
     * This uses an unstable api for preventing a player from connecting in the first place
     * If this ever becomes a problem, either update it to a new, supported method
     * Or just resort to a kick-on-join thing
     */
    @EventHandler
    @Suppress("UnstableApiUsage")
    fun playerLoginEvent(event: PlayerConnectionValidateLoginEvent) {
        val connection = event.connection
        if(connection !is PlayerLoginConnection) return

        val uuid = connection.authenticatedProfile?.id

        runBlocking {
            if(!databaseController.isWhitelisted(uuid.toString())) {
                event.kickMessage(
                    Component.text("———————————————————————————————", NamedTextColor.RED)
                        .append(Component.newline())
                        .append(Component.text("You are not whitelisted!", NamedTextColor.RED))
                        .append(Component.newline())
                        .append(Component.text("———————————————————————————————", NamedTextColor.RED))
                )
            }
        }
    }

    @EventHandler
    fun blockBreakEvent(event: BlockBreakEvent) {
        if(event.block.location.world == Bukkit.getWorld(lobbyWorld)!! && event.player.gameMode != GameMode.CREATIVE)
            event.isCancelled = true
    }

    val channels: HashMap<Player, ChatChannel> = HashMap()
    @EventHandler
    fun playerMessageEvent(event: AsyncChatEvent) {
        event.isCancelled = true

        if(isChatMuted) {
            event.player.sendMessage(Format.error("The chat is currently muted!"))
            return
        }

        val channel = channels[event.player] ?: ChatChannel.LOCAL

        val viewers = Bukkit.getOnlinePlayers().filter {
            channel.canSee(event.player, it)
        }

        Audience.audience(viewers).sendMessage(
            channel.format(event.player, event.message())
        )
    }

    fun muteChat() {
        isChatMuted = true
        Bukkit.broadcast(Format.info("The chat has been muted!"))
    }

    fun unmuteChat() {
        isChatMuted = false
        Bukkit.broadcast(Format.info("The chat has been unmuted!"))
    }

    fun updateNametagVisibility(owner: Player? = null) {
        val tags = if(owner == null) nameTags else hashMapOf(owner to nameTags[owner])

        tags.forEach {
            Bukkit.getOnlinePlayers().forEach { plr ->
                if(
                    currentNametagMode.canSee(plr, it.key)
                    && !spectatorController.spectators.contains(it.key)
                    && !hiddenPlayers.contains(it.key)
                    && it.key != plr
                ) {
                    plr.showEntity(TreeTumblers.plugin, it.value)
                } else {
                    plr.hideEntity(TreeTumblers.plugin, it.value)
                }
            }
        }
    }

    fun reloadNametags() {
        Bukkit.getOnlinePlayers().forEach {
            reloadNametag(it)
        }
    }

    fun removeNametag(player: Player) {
        val tag = nameTags[player] ?: return
        tag.remove()
        nameTags.remove(player)
    }

    fun reloadNametag(player: Player) {
        if(nameTags.containsKey(player)) {
            nameTags[player]!!.remove()
            nameTags.remove(player)
        }

        player.world.spawn(player.location.clone().add(0.0, 2.0, 0.0), TextDisplay::class.java) {
            nameTags.put(player, it)

            it.text(player.formattedName)
            it.isVisibleByDefault = false
            it.billboard = Display.Billboard.CENTER

            it.isSeeThrough = !player.isSneaking
            it.textOpacity = (if(player.isSneaking) 0x55 else 0xFF).toByte()

            it.transformation = Transformation(
                Vector3f(0f, 0.3f, 0f),
                AxisAngle4f(),
                Vector3f(1f, 1f, 1f),
                AxisAngle4f()
            )

            player.addPassenger(it)
            updateNametagVisibility(player)
        }
    }

    @EventHandler
    fun playerCrouchEvent(event: PlayerToggleSneakEvent) {
        val nameTag = nameTags[event.player] ?: return
        nameTag.isSeeThrough = !event.isSneaking
        nameTag.textOpacity = (if(event.isSneaking) 0x55 else 0xFF).toByte()
    }

    enum class NametagMode {
        ALL {
            override fun canSee(source: Player, viewer: Player): Boolean {
                return true
            }
        },
        TEAM {
            override fun canSee(source: Player, viewer: Player): Boolean {
                return (source.tumblingPlayer.team == viewer.tumblingPlayer.team) || !viewer.tumblingPlayer.team.playingTeam
            }
        },
        NONE {
            override fun canSee(source: Player, viewer: Player): Boolean {
                return false
            }
        };

        abstract fun canSee(source: Player, viewer: Player): Boolean
    }


    enum class ChatChannel(val channelName: String, val color: TextColor) {
        LOCAL("Local", NamedTextColor.WHITE) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return true
            }

            override fun canSend(player: Player): Boolean {
                return true
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}><sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        },
        TEAM("Team", TextColor.fromHexString("#34d031")!!) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return sender?.tumblingPlayer?.team == receiver.tumblingPlayer.team
            }

            override fun canSend(player: Player): Boolean {
                return player.tumblingPlayer.team.playingTeam
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}>[Team] <sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        },
        STAFF("Staff", TextColor.fromHexString("#ff3c50")!!) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return receiver.hasPermission("tumbling.dev") || receiver.hasPermission("tumbling.organizer")
            }

            override fun canSend(player: Player): Boolean {
                return canSee(null, player)
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<color:${color.asHexString()}>[Staff] <sender>: <message></color>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        },
        // maybe add some protection so you're not wc-ing the announcement channel
        ANNOUNCEMENT("Announcement", NamedTextColor.GOLD) {
            override fun canSee(sender: Player?, receiver: Player): Boolean {
                return true
            }

            override fun canSend(player: Player): Boolean {
                return player.hasPermission("tumbling.dev") || player.hasPermission("tumbling.organizer")
            }

            override fun format(sender: Player, message: Component): Component {
                return Format.mm(
                    "<aqua><line:30></aqua><br><br><br><sender>: <message><br><br><br><aqua><line:30></aqua>",
                    Placeholder.component("sender", sender.formattedName),
                    Placeholder.component("message", message)
                )
            }
        };

        abstract fun canSee(sender: Player?, receiver: Player): Boolean
        abstract fun canSend(player: Player): Boolean
        abstract fun format(sender: Player, message: Component): Component
    }
}