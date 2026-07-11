package xyz.devcmb.tumblers.controllers.player

import net.kyori.adventure.text.Component
import org.bukkit.Bukkit
import org.bukkit.entity.Display
import org.bukkit.entity.Player
import org.bukkit.entity.TextDisplay
import org.bukkit.event.EventHandler
import org.bukkit.event.entity.EntityDamageEvent
import org.bukkit.event.entity.EntityPotionEffectEvent
import org.bukkit.event.entity.EntityRegainHealthEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.bukkit.event.player.PlayerToggleSneakEvent
import org.bukkit.potion.PotionEffectType
import org.bukkit.util.Transformation
import org.joml.AxisAngle4f
import org.joml.Vector3f
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.IController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.events.LoggedOnTumblingPlayerReadyEvent
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.runTask
import xyz.devcmb.tumblers.util.toCenterXZLocation
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.ceil

@Controller
object NametagController : IController {
    val playerTags: HashMap<Player, PlayerOverheadTags> = HashMap()
    var currentTagMode: NametagMode = NametagMode.ALL
        set(value) {
            playerTags.forEach { updateTagVisibility(it.value) }
            field = value
        }

    override fun init() {
    }

    fun createPlayerTags(player: Player) {
        val nameTag = player.world.spawn(player.location.toCenterXZLocation().add(0.0, 2.3, 0.0), TextDisplay::class.java) {
            it.text(player.formattedName)
            player.addPassenger(it)

            setDefaultTextDisplayOptions(player, it)

            it.transformation = Transformation(
                Vector3f(0f, 0.3f, 0f),
                AxisAngle4f(),
                Vector3f(1f, 1f, 1f),
                AxisAngle4f()
            )
        }

        val healthBar = player.world.spawn(player.location.toCenterXZLocation().add(0.0, 2.6, 0.0), TextDisplay::class.java) {
            it.text(constructHealthBar(player))
            nameTag.addPassenger(it)

            setDefaultTextDisplayOptions(player, it)

            it.transformation = Transformation(
                Vector3f(0f, 0.6f, 0f),
                AxisAngle4f(),
                Vector3f(1f, 1f, 1f),
                AxisAngle4f()
            )
        }

        val tags = PlayerOverheadTags(player, nameTag, healthBar)
        playerTags[player] = tags
        runTask {
            updateTagVisibility(tags)
        }
    }

    fun removePlayerTags(player: Player) {
        playerTags[player]?.nameTag?.remove()
        playerTags[player]?.healthBar?.remove()
        playerTags.remove(player)
    }

    fun refreshAllTags() {
        Bukkit.getOnlinePlayers().forEach {
            refreshPlayerTags(it)
        }
    }

    fun refreshPlayerTags(player: Player) {
        removePlayerTags(player)
        createPlayerTags(player)
    }

    fun setDefaultTextDisplayOptions(player: Player, display: TextDisplay) {
        display.isVisibleByDefault = false
        display.billboard = Display.Billboard.CENTER

        display.viewRange = 0.15f

        display.isSeeThrough = !player.isSneaking
        display.textOpacity = (if(player.isSneaking) 0x55 else 0xFF).toByte()
    }

    fun constructHealthBar(player: Player): Component {
        var component = Component.empty()
            .append(Format.mm("<glyph:icon/hp_bar/base>"))
            .append(UserInterfaceUtility.negativeSpace(1))

        val hearts = ceil(player.health / 2.0).toInt().coerceIn(1, 10)

        repeat(4) { index ->
            val threshold = (index + 1) * 2

            component = component.append(
                when {
                    hearts > threshold -> Format.mm("<glyph:icon/hp_bar/full>")
                    hearts == threshold -> Format.mm("<glyph:icon/hp_bar/half>")
                    else -> Format.mm("<glyph:icon/hp_bar/empty>")
                }
            )

            component = component.append(UserInterfaceUtility.negativeSpace(1))
        }

        component = component.append(
            if (hearts >= 10)
                Format.mm("<glyph:icon/hp_bar/end_full>")
            else
                Format.mm("<glyph:icon/hp_bar/end_empty>")
        )

        return component
    }

    fun updateTagVisibility(tags: PlayerOverheadTags, viewer: Player? = null) {
        val players = if(viewer == null) Bukkit.getOnlinePlayers() else setOf(viewer)

        players.forEach {
            if(canSeeNametag(it, tags.player)) {
                it.showEntity(TreeTumblers.plugin, tags.nameTag)
            } else {
                it.hideEntity(TreeTumblers.plugin, tags.nameTag)
            }

            if(
                canSeeNametag(it, tags.player)
                && GameController.activeGame != null
                && Flag.HIDE_HEALTH_INDICATOR !in GameController.activeGame!!.data.flags
            ) {
                it.showEntity(TreeTumblers.plugin, tags.healthBar)
            } else {
                it.hideEntity(TreeTumblers.plugin, tags.healthBar)
            }
        }
    }

    fun updateTagVisibility(player: Player, viewer: Player? = null)
        = updateTagVisibility(playerTags[player]!!, viewer)

    fun canSeeNametag(viewer: Player, taggedPlayer: Player): Boolean {
        return currentTagMode.canSee(taggedPlayer, viewer)
                && !SpectatorController.spectators.contains(taggedPlayer)
                && !taggedPlayer.hasPotionEffect(PotionEffectType.INVISIBILITY)
                && !PlayerController.hiddenPlayers.contains(taggedPlayer)
                && taggedPlayer != viewer
    }

    fun updateHealthBar(player: Player) {
        val playerHealthBar = playerTags[player]?.healthBar ?: return
        playerHealthBar.text(constructHealthBar(player))
    }

    @EventHandler
    fun playerReady(event: LoggedOnTumblingPlayerReadyEvent) {
        val player = event.bukkitPlayer
        createPlayerTags(player)

        runTask {
            playerTags.forEach {
                updateTagVisibility(it.value, player)
            }
        }
    }

    @EventHandler
    fun playerLeave(event: PlayerQuitEvent) {
        val player = event.player
        val tags = playerTags[player] ?: return

        tags.healthBar.remove()
        tags.nameTag.remove()
    }

    @EventHandler
    fun playerCrouchEvent(event: PlayerToggleSneakEvent) {
        val playerTags = playerTags[event.player] ?: return

        val tags = listOf(playerTags.nameTag, playerTags.healthBar)
        tags.forEach {
            it.isSeeThrough = !event.isSneaking
            it.textOpacity = (if(event.isSneaking) 0x55 else 0xFF).toByte()
        }
    }

    @EventHandler
    fun playerDamageEvent(event: EntityDamageEvent) {
        val player = event.entity as? Player ?: return
        updateHealthBar(player)
    }

    @EventHandler
    fun playerHealEvent(event: EntityRegainHealthEvent) {
        val player = event.entity as? Player ?: return
        updateHealthBar(player)
    }

    @EventHandler
    fun playerEffectEvent(event: EntityPotionEffectEvent) {
        val player = event.entity as? Player ?: return
        if((event.newEffect?.type == PotionEffectType.INVISIBILITY || event.oldEffect?.type == PotionEffectType.INVISIBILITY)) {
            updateTagVisibility(player)
        }
    }

    data class PlayerOverheadTags(val player: Player, val nameTag: TextDisplay, val healthBar: TextDisplay)

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
}