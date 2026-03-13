package xyz.devcmb.tumblers.controllers

import io.papermc.paper.connection.PlayerLoginConnection
import io.papermc.paper.event.connection.PlayerConnectionValidateLoginEvent
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.player.PlayerInteractEvent
import org.bukkit.event.player.PlayerJoinEvent
import org.bukkit.event.player.PlayerQuitEvent
import xyz.devcmb.tumblers.Constants
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.ui.PlayerUIController
import xyz.devcmb.tumblers.util.DebugUtil
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.item.AdvancedItemRegistry
import xyz.devcmb.tumblers.util.tumblingPlayer
import xyz.devcmb.tumblers.util.unpackCoordinates

@Controller("playerController", Controller.Priority.MEDIUM)
class PlayerController : IController {
    val players: ArrayList<TumblingPlayer> = ArrayList()
    val playerUIControllers: HashMap<Player, PlayerUIController> = HashMap()

    val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }

    companion object {
        @field:Configurable("lobby.world")
        var lobbyWorld = "world"

        @field:Configurable("lobby.position")
        var lobbySpawn: List<Double> = listOf(0.0, 127.0, 0.0)
    }

    override fun init() {
    }

    @EventHandler
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player
        player.inventory.clear()
        player.teleport(lobbySpawn.unpackCoordinates(Bukkit.getWorld(lobbyWorld)!!))

        event.joinMessage(Component.empty())
        playerUIControllers.put(player, PlayerUIController(player))

        if(Constants.IS_DEVELOPMENT) {
            DebugUtil.subscribe(player, DebugUtil.DebugLogLevel.WARNING)
            player.sendMessage(
                Component.text("Developer mode is active. You have automatically been subscribed to the warning debug channel.")
                    .color(NamedTextColor.YELLOW)
            )
        }

        TreeTumblers.pluginScope.launch {
            var data: TumblingPlayer
            try {
                data = databaseController.getPlayerData(player)
                players.add(data)
            } catch(e: Exception) {
                DebugUtil.severe("Failed to get player data for ${player.name}: ${e.message}")
                player.kick(Component.text("Data failed to load. Please try again or contact an admin.", NamedTextColor.RED))
            }

            player.playerListName(Format.formatPlayerName(player))
            Bukkit.broadcast(
                Component.text("[").color(NamedTextColor.GRAY)
                    .append(Component.text("+").color(NamedTextColor.GREEN))
                    .append(Component.text("] ").color(NamedTextColor.GRAY))
                    .append(Format.formatPlayerName(player).color(NamedTextColor.WHITE))
            )

        }
    }

    @EventHandler
    fun playerQuit(event: PlayerQuitEvent) {
        val player = event.player
        val tumblingPlayer = player.tumblingPlayer ?: return

        event.quitMessage(
            Component.text("[").color(NamedTextColor.GRAY)
                .append(Component.text("-").color(NamedTextColor.RED))
                .append(Component.text("] ").color(NamedTextColor.GRAY))
                .append(Format.formatPlayerName(player).color(NamedTextColor.WHITE))
        )

        TreeTumblers.pluginScope.launch {
            try {
                databaseController.replicatePlayerData(tumblingPlayer)
                players.remove(tumblingPlayer)
            } catch(e: Exception) {
                DebugUtil.severe("Failed to replicate player data for ${player.name}: ${e.message}")
            }
        }
    }

    @EventHandler
    fun playerInteract(event: PlayerInteractEvent) {
        AdvancedItemRegistry.handleInteract(event)
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
}