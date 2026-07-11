package xyz.devcmb.tumblers.controllers.games.flood_escape

import io.papermc.paper.util.Tick
import kotlinx.coroutines.delay
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import net.kyori.adventure.title.Title
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.controllers.games.flood_escape.FloodEscapeController.MovementDirection
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.suspendSync

object FloodEscapeData : GameData(
    id = "flood_escape",
    name = "Flood Escape",
    votable = true,
    maps = setOf(Map("sewer")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/flood_escape_icon>"))
                .append(Component.text(" Flood Escape")),
            "cutscene.start"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, you need to complete a set of <green>obstacles</green> in order to outrun the <aqua>flood</aqua> speeding towards you."),
            "cutscene.first"
        ) {
            delay(6000)
        },
        CutsceneStep(
            Format.mm(
                "After <yellow>20 seconds</yellow>, the water will start inching towards the player.<br>" +
                    "Every <green>60 seconds</green> after that, the water will start speeding up"
            ),
            "cutscene.second"
        ) {
            delay(4000)
            val game = game as FloodEscapeController
            val water = suspendSync {
                game.spawnWater()
            }

            val direction = MovementDirection.entries
                .find { entry -> entry.identifier == (it.data.getString("water.movement_direction") ?: throw GameControllerException("Water movement direction not found")) }
                ?: throw GameControllerException("Water movement direction matching ${it.data.getString("water.movement_direction")} could not be found")

            repeat(40) {
                suspendSync {
                    water.teleport(direction.increase(
                        water.location,
                        (game.waterSpeed / 20).toFloat()
                    ))
                }
                delay(50)
            }

            suspendSync {
                water.remove()
            }
        },
        CutsceneStep(
            Format.mm(
                "Each obstacle has a <yellow>type</yellow>, giving you a different set of items whenever you reach it.<br>" +
                    "As you go further, the <red>difficulty</red> of the obstacles will steadily increase, " +
                        "from <green>easy</green>, to <yellow>medium</yellow>, to <color:${Team.ORANGE.color}>hard</color>, and finally to <red>extreme</red>."),
            "cutscene.third"
        ) {
            var currentType: FloodEscapeController.ObstacleType? = null
            FloodEscapeController.ObstacleType.entries.reversed().forEach {
                var component = Component.empty()
                if(currentType?.icon != null) {
                    component = component.append(
                        Format.mm("<red>[- <icon>]</red>", Placeholder.component("icon", currentType.icon))
                    )
                }

                if(it.icon != null) {
                    component = component.append(
                        Format.mm("${if(currentType?.icon != null) " " else ""}<green>[+ <icon>]</green>", Placeholder.component("icon", it.icon))
                    )
                }

                Audience.audience(observers).showTitle(Title.title(
                    Component.empty(),
                    component,
                    Title.Times.times(Tick.of(5), Tick.of(45), Tick.of(5))
                ))

                currentType = it
                delay(1500)
            }

            delay(1000)

            Audience.audience(observers).showTitle(Title.title(
                Component.empty(),
                Format.error("Difficulty increasing!"),
                Title.Times.times(Tick.of(0), Tick.of(45), Tick.of(5))
            ))

            delay(4000)
        },
        CutsceneStep.GLHF
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP,
        Flag.DISABLE_NATURAL_REGENERATION,
        Flag.HIDE_HEALTH_INDICATOR
    ),
    icon = Format.mm("<glyph:game/flood_escape_icon>"),
    logo = Format.mm("<glyph:game/flood_escape_logo>"),
    tabLogo = Format.mm("<glyph:game/flood_escape_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    scores = hashMapOf(
        FloodEscapeController.FloodEscapeScoreSource.COMPLETE_OBSTACLE to 2,
        FloodEscapeController.FloodEscapeScoreSource.OUTLAST_OPPONENT to 25
    ),
    scoreboard = FloodEscapeScoreboard::class,
    spawns = FloodEscapeSpawn.entries
)