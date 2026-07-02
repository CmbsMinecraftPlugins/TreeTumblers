package xyz.devcmb.tumblers.ui

import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.roundToInt

object UserInterfaceUtility {
    val SPACES = NamespacedKey(TreeTumblers.NAMESPACE, "spaces")
    val ICONS = NamespacedKey(TreeTumblers.NAMESPACE, "icons")

    // item model key
    val FLAT_SKULL = NamespacedKey(TreeTumblers.NAMESPACE, "flat_skull")

    val CLOCK = Format.mm("<glyph:icon/timer>")

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

    val POSITIVE_ADVANCES: HashMap<Int, String> = hashMapOf(
        -1 to "\uE000",
        -5 to "\uE001",
        -10 to "\uE002",
        -15 to "\uE003",
        -20 to "\uE004",
        -25 to "\uE005",
        -30 to "\uE006",
        -35 to "\uE007",
        -40 to "\uE008",
        -45 to "\uE009",
        -50 to "\uE00A",
        -55 to "\uE00B",
        -60 to "\uE00C",
        -65 to "\uE00D",
        -70 to "\uE00E",
        -75 to "\uE00F",
        -80 to "\uE010",
        -85 to "\uE011",
        -90 to "\uE012",
        -95 to "\uE013",
        -100 to "\uE014",
        -105 to "\uE015",
        -110 to "\uE016",
        -115 to "\uE017",
        -120 to "\uE018",
        -125 to "\uE019",
        -130 to "\uE01A",
        -135 to "\uE01B",
        -140 to "\uE01C",
        -145 to "\uE01D",
        -150 to "\uE01E",
        -155 to "\uE01F",
        -160 to "\uE020",
        -165 to "\uE021",
        -170 to "\uE022",
        -175 to "\uE023",
        -180 to "\uE024",
        -185 to "\uE025",
        -190 to "\uE026",
        -195 to "\uE027",
        -200 to "\uE028",
        -205 to "\uE029",
        -210 to "\uE02A",
        -215 to "\uE02B",
        -220 to "\uE02C",
        -225 to "\uE02D",
        -230 to "\uE02E",
        -235 to "\uE02F",
        -240 to "\uE030",
        -245 to "\uE031",
        -250 to "\uE032",
        -255 to "\uE033",
        -260 to "\uE034",
        -265 to "\uE035",
        -270 to "\uE036",
        -275 to "\uE037",
        -280 to "\uE038",
        -285 to "\uE039",
        -290 to "\uE03A",
        -295 to "\uE03B",
        -300 to "\uE03C"
    )

    const val FULL_INVENTORY_NEGATIVE_ADVANCE = 170

    fun negativeSpace(targetPixels: Int): Component = space(targetPixels, NEGATIVE_ADVANCES)
    fun positiveSpace(targetPixels: Int): Component = space(targetPixels, POSITIVE_ADVANCES)

    private fun space(target: Int, glyphs: HashMap<Int, String>): Component {
        if(target <= 0) return Component.empty()

        var remaining = target
        val result = StringBuilder()
        val sorted = glyphs.keys.sortedBy { it }

        while (remaining > 0) {
            var matched = false

            for (value in sorted) {
                val abs = -value
                if (remaining >= abs) {
                    result.append(glyphs[value])
                    remaining -= abs
                    matched = true
                    break
                }
            }

            if (!matched) {
                throw IllegalArgumentException("Cannot perfectly match spacing for $target px")
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
        PlayerController.playerUIControllers.forEach { (_, controller) ->
            if(controller.currentInventory != null && controller.currentInventory!!.second == id) {
                controller.currentInventory!!.first.redrawComplete()
            }
        }
    }

    fun getTeamScoresComponent(player: Player, activeGame: AbstractGame): ArrayList<Component> {
        val leaderboard: ArrayList<Component> = arrayListOf()
        if(EventController.scoresHidden) {
            repeat(4) {
                leaderboard.add(Format.mm(MiniMessagePlaceholders.Game.HIDDEN_TEAM_SCOREBOARD_PLACEMENT))
            }

            return leaderboard
        }

        val placements = activeGame.getTeamPlacements().take(3)
        if(placements.find { it.first == player.tumblingPlayer.team } == null && player.tumblingPlayer.team.playingTeam) {
            val top = placements.first()
            leaderboard.add(Format.mm(
                MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                Placeholder.unparsed("placement", top.second.toString()),
                Placeholder.component("team", top.first.formattedName),
                Placeholder.parsed("score", activeGame.teamScores[top.first]!!.toString())
            ))

            leaderboard.add(Component.empty())

            val placements = activeGame.getTeamPlacements()
            var teamPlacementIndex = placements.indexOfFirst { it.first == player.tumblingPlayer.team }
            if(teamPlacementIndex == -1) teamPlacementIndex = placements.size + 1

            val teams = arrayListOf(
                placements.getOrNull(teamPlacementIndex - 1),
                placements[teamPlacementIndex],
                placements.getOrNull(teamPlacementIndex + 1)
            )

            val nextPlacement = placements.getOrNull(teamPlacementIndex - 2)
            if(teams.filterNotNull().size != 3 && nextPlacement != null) {
                teams.addFirst(nextPlacement)
            }

            teams.forEach {
                if(it == null) return@forEach

                val (team, placement) = it
                leaderboard.add(Format.mm(
                    MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.unparsed("placement", placement.toString()),
                    Placeholder.component("team", team.formattedName),
                    Placeholder.parsed("score", activeGame.teamScores[team]!!.toString())
                ))
            }
        } else {
            val top4 = activeGame.getTeamPlacements().take(4)
            top4.forEach { (team, placement) ->
                leaderboard.add(Format.mm(
                    MiniMessagePlaceholders.Game.TEAM_SCOREBOARD_PLACEMENT,
                    Placeholder.unparsed("placement", placement.toString()),
                    Placeholder.component("team", team.formattedName),
                    Placeholder.parsed("score", activeGame.teamScores[team]!!.toString())
                ))
            }
        }

        return leaderboard
    }

    fun getIndividualScoreComponent(player: Player, activeGame: AbstractGame): Component {
        val tumblingPlayer = player.tumblingPlayer
        val playerPlacement = activeGame.getIndividualPlacements().find { it.first == tumblingPlayer }
            ?: Pair(tumblingPlayer,activeGame.getIndividualPlacements().size + 1)

        return Format.mm(
            if(!EventController.scoresHidden) MiniMessagePlaceholders.Game.INDIVIDUAL_SCOREBOARD_PLACEMENT
                else MiniMessagePlaceholders.Game.HIDDEN_INDIVIDUAL_SCOREBOARD_PLACEMENT_WITH_SCORE,
            Placeholder.unparsed("placement", playerPlacement.second.toString()),
            Placeholder.parsed("head", "<head:${player.uniqueId}>"),
            Placeholder.component("name", player.formattedName),
            Placeholder.parsed("score", (activeGame.playerScores[tumblingPlayer] ?: 0).toString())
        )
    }

    fun empty(): ItemStack {
        return  ItemStack.of(Material.ECHO_SHARD).apply {
            setNoxesiumComponent(CommonItemComponentTypes.IMMOVABLE, com.noxcrew.noxesium.api.util.Unit.INSTANCE)
            itemMeta = itemMeta.also {
                it.itemModel = NamespacedKey(TreeTumblers.NAMESPACE, "empty")
            }
        }
    }

    fun customInventoryTitle(overlay: Component, title: Component): Component {
        return Component.empty()
            .append(negativeSpace(8))
            .append(overlay)
            .append(negativeSpace(FULL_INVENTORY_NEGATIVE_ADVANCE))
            .append(title)
    }

    // this kinda works?
    // math is prob wrong though
    fun doubleLinedText(
        component1: Component,
        component2: Component,
        component1Offset: Int = 0,
        component2Offset: Int = 0,
    ): Component {
        val (c1Text, c2Text) = (PlainTextComponentSerializer.plainText().serialize(component1) to PlainTextComponentSerializer.plainText().serialize(component2))
        val (c1Length, c2Length) = (getPixelWidth(c1Text) + component1Offset) to (getPixelWidth(c2Text) + component2Offset)

        var fullOffset: Component
        var lowerTextOffset: Double

        if(c1Length > c2Length) {
            lowerTextOffset = c1Length + (c1Length - c2Length)/2.0
            fullOffset = negativeSpace((c1Length - c2Length)/2)
        } else {
            lowerTextOffset = c2Length - (c2Length - c1Length)/2.0
            fullOffset = positiveSpace((c2Length - c1Length)/2)
        }

        return Component.empty()
            .append(fullOffset)
            .append(component1)
            .append(negativeSpace(lowerTextOffset.roundToInt()))
            .append(component2.font(NamespacedKey(TreeTumblers.NAMESPACE, "default_shift/ascent_-5")))
    }
}