package xyz.devcmb.tumblers.ui

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.controllers.PlayerController
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.roundToInt

object UserInterfaceUtility {
    val SPACES = NamespacedKey("tumbling", "spaces")
    val WARNINGS = NamespacedKey("tumbling", "warnings")
    val HUD = NamespacedKey("tumbling", "hud")
    val ICONS = NamespacedKey("tumbling", "icons")

    // item model key
    val FLAT_SKULL = NamespacedKey("tumbling", "flat_skull")

    val NEGATIVE_ADVANCES: HashMap<Int, String> = hashMapOf(
        -1 to "\uF000",
        -5 to "\uF001",
        -10 to "\uF002",
        -15 to "\uF003",
        -20 to "\uF004",
        -25 to "\uF005",
        -30 to "\uF006",
        -35 to "\uF007",
        -40 to "\uF008",
        -45 to "\uF009",
        -50 to "\uF00A",
        -55 to "\uF00B",
        -60 to "\uF00C",
        -65 to "\uF00D",
        -70 to "\uF00E",
        -75 to "\uF00F",
        -80 to "\uF010",
        -85 to "\uF011",
        -90 to "\uF012",
        -95 to "\uF013",
        -100 to "\uF014",
        -105 to "\uF015",
        -110 to "\uF016",
        -115 to "\uF017",
        -120 to "\uF018",
        -125 to "\uF019",
        -130 to "\uF01A",
        -135 to "\uF01B",
        -140 to "\uF01C",
        -145 to "\uF01D",
        -150 to "\uF01E",
        -155 to "\uF01F",
        -160 to "\uF020",
        -165 to "\uF021",
        -170 to "\uF022",
        -175 to "\uF023",
        -180 to "\uF024",
        -185 to "\uF025",
        -190 to "\uF026",
        -195 to "\uF027",
        -200 to "\uF028",
        -205 to "\uF029",
        -210 to "\uF02A",
        -215 to "\uF02B",
        -220 to "\uF02C",
        -225 to "\uF02D",
        -230 to "\uF02E",
        -235 to "\uF02F",
        -240 to "\uF030",
        -245 to "\uF031",
        -250 to "\uF032",
        -255 to "\uF033",
        -260 to "\uF034",
        -265 to "\uF035",
        -270 to "\uF036",
        -275 to "\uF037",
        -280 to "\uF038",
        -285 to "\uF039",
        -290 to "\uF03A",
        -295 to "\uF03B",
        -300 to "\uF03C"
    )

    fun negativeSpace(targetPixels: Int): Component {
        var remaining = targetPixels
        val result = StringBuilder()
        val sorted = NEGATIVE_ADVANCES.keys.sortedBy { it }

        while (remaining > 0) {
            var matched = false

            for (value in sorted) {
                val abs = -value
                if (remaining >= abs) {
                    result.append(NEGATIVE_ADVANCES[value])
                    remaining -= abs
                    matched = true
                    break
                }
            }

            if (!matched) {
                throw IllegalArgumentException("Cannot perfectly match spacing for $targetPixels px")
            }
        }

        return Component.text(result.toString(), NamedTextColor.WHITE).font(SPACES)
    }

    // a link to the research I did to get these numbers
    // https://confused-animal-c90.notion.site/Minecraft-resource-pack-UI-3206aa5edc9980e9a296d96d9ec07142
    fun backgroundTextCenter(background: Component, textComponent: Component, stringText: String, bgSize: Double, offset: Double = 0.0): Component {
        val textLength: Double = (getPixelWidth(stringText) + offset)

        // very important: if these are not roundToInt, it could be offset (I found this out the hard way)
        val bgOffset = (textLength+((bgSize - textLength)/2)).roundToInt()
        val fullOffset = ((bgSize - textLength) / 2).roundToInt()

        return Component.empty()
            .append(negativeSpace(fullOffset))
            .append(background)
            .append(negativeSpace(bgOffset))
            .append(textComponent)
    }

    fun getPixelWidth(text: String): Int {
        return text.sumOf { ch -> DefaultFontGlyphs.entries.find { it.char == ch }?.width ?: 0 } + (text.length - 1)
    }

    fun refreshAll(id: String) {
        val playerController = ControllerDelegate.getController("playerController") as PlayerController
        playerController.playerUIControllers.forEach { player, controller ->
            val inv = controller.inventories.find { it.id == id }
            require(inv != null) { "Inventory with an id of $id was not found for ${player.name}" }
            inv.inventory.reload()
        }
    }

    fun getTeamScoresComponent(player: Player, activeGame: GameBase): ArrayList<Component> {
        val leaderboard: ArrayList<Component> = arrayListOf()
        val placements = activeGame.getTeamPlacements().take(4)

        placements.forEach { (team, placement) ->
            leaderboard.add(Format.mm(
                MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                Placeholder.unparsed("placement", placement.toString()),
                Placeholder.component("team", team.formattedName),
                Placeholder.parsed("score", activeGame.teamScores[team]!!.toString())
            ))
        }

        if(placements.find { it.first == player.tumblingPlayer.team } == null) {
            val teamPlacement = activeGame.getTeamPlacements().find { it.first == player.tumblingPlayer.team }!!
            leaderboard.add(Component.empty())
            leaderboard.add(Format.mm(
                MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                Placeholder.unparsed("placement", teamPlacement.second.toString()),
                Placeholder.component("team", teamPlacement.first.formattedName),
                Placeholder.parsed("score", activeGame.teamScores[teamPlacement.first]!!.toString())
            ))
        }

        return leaderboard
    }

    fun getIndividualScoreComponent(player: Player, activeGame: GameBase): Component {
        val tumblingPlayer = player.tumblingPlayer
        val playerPlacement = activeGame.getIndividualPlacements().find { it.first == tumblingPlayer }!!

        return Format.mm(
            MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT,
            Placeholder.unparsed("placement", playerPlacement.second.toString()),
            Placeholder.parsed("head", "<head:${player.uniqueId}>"),
            Placeholder.component("name", player.formattedName),
            Placeholder.parsed("score", (activeGame.playerScores[tumblingPlayer] ?: 0).toString())
        )
    }
}