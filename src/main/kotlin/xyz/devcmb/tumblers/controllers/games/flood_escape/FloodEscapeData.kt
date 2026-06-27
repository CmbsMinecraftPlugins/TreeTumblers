package xyz.devcmb.tumblers.controllers.games.flood_escape

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.games.flood_escape.FloodEscapeController.Companion.font
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map

object FloodEscapeData : GameData(
    id = "flood_escape",
    name = "Flood Escape",
    votable = true,
    maps = setOf(Map("sewer")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(font))
                .append(Component.text(" Flood Escape")),
            "cutscene.start"
        ) {
            delay(5000)
        }
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP,
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    tabLogo = Component.text("\uEA02").font(font)
        .shadowColor(ShadowColor.none()),
    scores = hashMapOf(),
    scoreboard = "floodEscapeScoreboard",
    spawns = FloodEscapeSpawns.entries
)