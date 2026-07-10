package xyz.devcmb.tumblers.data

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.player.PlayerEventController
import xyz.devcmb.tumblers.util.Format
import java.sql.Timestamp
import java.util.UUID

data class TumblingPlayer(
    val uuid: UUID,
) {
    var bukkitPlayer: Player? = null

    // Values get updated on creation
    // its done this way so changing the score doesn't change the object signature
    // (it did that when the score was in the constructor)
    var team: Team = Team.SPECTATORS
    var name: String = "Player"
    var score: Int = 0
    val badges: HashMap<BadgeController.Badge, Timestamp> = HashMap()

    val currentScoreboards: ArrayList<String> = ArrayList()
    val currentBossbars: ArrayList<String> = ArrayList()
    val currentActionBars: ArrayList<String> = ArrayList()

    val isOnline: Boolean
        get() {
            return bukkitPlayer != null && bukkitPlayer!!.isOnline
        }

    val formattedName: Component
        get() {
            return Format.formatPlayerName(this)
        }

    val eliminatedName: Component
        get() {
            return Format.mm(
                "<white><icon></white> <gray>${name}</gray>",
                Placeholder.component("icon", team.formattedIcon)
            )
        }

    fun showKill(player: TumblingPlayer, score: Int?) {
        PlayerEventController.addEvent(this, PlayerEventController.Event.KillEvent(this, player, score))
    }
}