package xyz.devcmb.tumblers.controllers.player

import com.github.retrooper.packetevents.PacketEvents
import com.github.retrooper.packetevents.protocol.player.Equipment
import com.github.retrooper.packetevents.protocol.player.EquipmentSlot
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerEntityEquipment
import com.noxcrew.noxesium.paper.component.noxesiumPlayer
import io.github.retrooper.packetevents.util.SpigotConversionUtil
import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import io.papermc.paper.event.player.AsyncChatEvent
import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.TextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.GameMode
import org.bukkit.Material
import org.bukkit.attribute.Attribute
import org.bukkit.damage.DamageType
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.PlayerDeathEvent
import org.bukkit.event.inventory.InventoryClickEvent
import org.bukkit.event.player.PlayerDropItemEvent
import org.bukkit.event.player.PlayerExpChangeEvent
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerItemConsumeEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerStatisticIncrementEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.inventory.ItemStack
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.DatabaseController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.event.HubController
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.ui.PlayerUIController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.item.AdvancedItemRegistry
import xyz.devcmb.tumblers.util.runTask
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.tumblingPlayer
import java.util.UUID

@Controller(Controller.Priority.MEDIUM)
object PlayerController : IController {
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

    override fun init() {
        TreeTumblers.pluginScope.launch {
            players = DatabaseController.getAllPlayerData()
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

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.inventory.clear()
        player.gameMode = GameMode.ADVENTURE
        player.isFlying = false
        player.isGlowing = false
        player.allowFlight = false
        player.level = 0
        player.exp = 1f
        player.clearActivePotionEffects()

        runTaskLater(40) {
            if(player.noxesiumPlayer == null) {
                player.sendMessage(Format.warning(Format.mm(
                    "We highly recommend downloading the " +
                            "<hover:show_text:'<green>Click to Download</green>'><click:open_url:https://modrinth.com/mod/noxesium><green>[Noxesium Mod]</green></click></hover>" +
                            " to make your experience the best it can be during the event. This mod synchronizes certain events to make them work identically regardless of ping."
                )))
            }
        }

        player.vehicle?.let {
            player.leaveVehicle()
            it.remove()
        }

        runTask {
            HubController.spawnHub(player)
            reloadNametag(player)
            nameTags.forEach { (otherPlr, tag) ->
                if (canSeeNametag(player, otherPlr)) {
                    player.showEntity(TreeTumblers.plugin, tag)
                }
            }
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

        event.joinMessage(null)
        playerUIControllers.forEach { it.value.playerJoin(player) }
        playerUIControllers[player] = PlayerUIController(player)

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
        Bukkit.broadcast(Format.mm("<white>(<green>+</green>)</white> <player:${tumblingPlayer.uuid}>"))
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

        event.quitMessage(null)
        Bukkit.broadcast(Format.mm("<white>(<red>-</red>)</white> <player:${tumblingPlayer.uuid}>"))

        tumblingPlayer.bukkitPlayer = null
    }

    @EventHandler
    fun playerEarnXPEvent(event: PlayerExpChangeEvent) {
        event.amount = 0
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
    fun inventoryClickEvent(event: InventoryClickEvent) {
        AdvancedItemRegistry.handleInventoryClickEvent(event)
    }

    @EventHandler
    fun playerKillEvent(event: PlayerDeathEvent) {
        val killed = event.player
        val killer = killed.killer ?: return

        val currentGame = GameController.activeGame
        val score = currentGame?.getScoreSource(CommonScoreSource.KILL)

        killer.tumblingPlayer.showKill(killed.tumblingPlayer, if (score != null && score > 0) score else null)
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
        if(!DatabaseController.isConnected()) {
            event.kickMessage(Format.mm("<red><st>${" ".repeat(60)}</st><br><br>" +
                    "Database initialization failed<br><br>" +
                    "<st>${" ".repeat(60)}</st></red>"))
            return
        }

        if (!players.any { it.uuid == uuid }) {
            event.kickMessage(Format.mm("<green><st>${" ".repeat(60)}</st><br><br>" +
                    "You aren't whitelisted!<br><br>" +
                    "<st>${" ".repeat(60)}</st></green>"))
        }
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
                if(canSeeNametag(plr, it.key)) {
                    plr.showEntity(TreeTumblers.plugin, it.value)
                } else {
                    plr.hideEntity(TreeTumblers.plugin, it.value)
                }
            }
        }
    }

    fun canSeeNametag(viewer: Player, taggedPlayer: Player): Boolean {
        return currentNametagMode.canSee(viewer, taggedPlayer)
                && !SpectatorController.spectators.contains(taggedPlayer)
                && !taggedPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
                && !hiddenPlayers.contains(taggedPlayer)
                && taggedPlayer != viewer
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
            nameTags[player] = it

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

    @EventHandler
    fun playerEffectEvent(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return

        if((event.newEffect?.type == PotionEffectType.INVISIBILITY || event.oldEffect?.type == PotionEffectType.INVISIBILITY)) {
            Bukkit.getOnlinePlayers().forEach {
                fun getNewStack(item: ItemStack): com.github.retrooper.packetevents.protocol.item.ItemStack {
                    return SpigotConversionUtil.fromBukkitItemStack(
                        if(
                            (event.newEffect?.type == PotionEffectType.INVISIBILITY && it.tumblingPlayer.team != player.tumblingPlayer.team)
                            || item.isEmpty || item.type == Material.AIR
                        ) ItemStack.of(Material.AIR)
                        else item
                    )
                }

                val equipment = listOf(
                    Equipment(EquipmentSlot.HELMET, getNewStack(player.inventory.helmet)),
                    Equipment(EquipmentSlot.CHEST_PLATE, getNewStack(player.inventory.chestplate)),
                    Equipment(EquipmentSlot.LEGGINGS, getNewStack(player.inventory.leggings)),
                    Equipment(EquipmentSlot.BOOTS, getNewStack(player.inventory.boots)),
                    Equipment(EquipmentSlot.MAIN_HAND, SpigotConversionUtil.fromBukkitItemStack(player.inventory.itemInMainHand)),
                    Equipment(EquipmentSlot.OFF_HAND, SpigotConversionUtil.fromBukkitItemStack(player.inventory.itemInOffHand))
                )

                val packet = WrapperPlayServerEntityEquipment(
                    player.entityId,
                    equipment
                )
                PacketEvents.getAPI().playerManager.sendPacket(it, packet)
            }

            if(nameTags.containsKey(player)) updateNametagVisibility(event.entity as Player)
        }
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