package xyz.devcmb.tumblers.engine

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.map.SpawnLocation
import xyz.devcmb.tumblers.engine.score.ScoreSource
import xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard
import xyz.devcmb.tumblers.util.Format
import kotlin.reflect.KClass

/**
 * Configuration data for a game implementation
 *
 * @param id The unique identifier of the game
 * @param name The name of the game for public-facing events (voting, etc.)
 * @param votable Whether this game is available for voting during the voting stage
 * @param maps A [Set] containing all the [xyz.devcmb.tumblers.engine.map.Map] instances
 * @param cutsceneSteps An [ArrayList] containing all the [xyz.devcmb.tumblers.engine.cutscene.CutsceneStep] instances
 * @param flags A [Set] of [xyz.devcmb.tumblers.engine.Flag] enums to determine certain shared behaviors
 * @param scores A [HashMap] of [ScoreSource]s to the amount of score they give
 * @param scoreboard The id of a [xyz.devcmb.tumblers.ui.scoreboard.HandledScoreboard]
 * @param badges The list of badges a game has that are displayed in the collection
 */
open class GameData(
    val id: String,
    val name: String,
    val votable: Boolean,
    val maps: Set<Map>,
    val cutsceneSteps: List<CutsceneStep>,
    val flags: Set<Flag>,
    val scores: HashMap<ScoreSource, Int>,
    val scoreboard: KClass<out HandledScoreboard>,
    val badges: List<BadgeController.Badge>? = null,
    val spawns: List<SpawnLocation>? = null,
    val listed: Boolean = true,
    val spectateInventory: String = "spectateInventory",

    val icon: Component = Format.mm("<glyph:game/${id}_icon>"),
    val logo: Component = Format.mm("<glyph:game/${id}_logo>"),
    val tabLogo: Component = Format.mm("<glyph:game/${id}_logo_14a_45h>").shadowColor(ShadowColor.shadowColor(0))
)