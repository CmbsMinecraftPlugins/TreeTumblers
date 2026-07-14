package xyz.devcmb.tumblers.ui

import com.noxcrew.noxesium.core.registry.CommonItemComponentTypes
import com.noxcrew.noxesium.paper.component.setNoxesiumComponent
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.controllers.player.UIController
import xyz.devcmb.tumblers.engine.base.AbstractGame
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.formattedName
import xyz.devcmb.tumblers.util.tumblingPlayer
import kotlin.math.roundToInt

object UserInterfaceUtility {
    val ICONS = NamespacedKey(TreeTumblers.NAMESPACE, "icons")

    // item model key
    val FLAT_SKULL = NamespacedKey(TreeTumblers.NAMESPACE, "flat_skull")

    val CLOCK = Format.mm("<glyph:icon/timer>")

    const val FULL_INVENTORY_NEGATIVE_ADVANCE = 170

    fun negativeSpace(targetPixels: Int): Component = space(-targetPixels)
    fun positiveSpace(targetPixels: Int): Component = space(targetPixels)

    private fun space(target: Int): Component = UIController.fUI.spacer.getSpacing(target.toDouble())

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
        if(!tumblingPlayer.team.playingTeam) return Format.mm("<gray>You are not participating</gray>")

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
            .append(centerInventoryTitle(title))
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

    fun timer(game: AbstractGame): Component {
        return Format.mm(
            "<color:${MiniMessagePlaceholders.Event.EVENT_COLOR}><white>${game.currentTimer?.title ?: "Timer"}:</white> <timer></color>",
            Placeholder.component("timer", game.currentTimer?.format() ?: Component.text("0:00"))
        )
    }

    fun centerInventoryTitle(title: Component): Component {
        val length = UIController.fUI.fontMeasurer.measureComponent(title)
        return Component.empty()
            .append(positiveSpace(81 - (length / 2).roundToInt()))
            .append(title.shadowColor(ShadowColor.shadowColor(0x3F, 0x3F, 0x3F, 0x3F)).font(NamespacedKey(TreeTumblers.NAMESPACE, "offset/default_offset_-3")))
    }
}