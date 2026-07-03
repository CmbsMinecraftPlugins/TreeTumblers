package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format

object BrawlData : GameData(
    id = "brawl",
    name = "Brawl",
    votable = true,
    maps = setOf(Map("district")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/brawl_icon>"))
                .append(Component.text(" Brawl")),
            "cutscene.start"
        ) { map ->
            delay(5000)
        },
    ),
    flags = setOf(),
    scores = hashMapOf(),
    icon = Format.mm("<glyph:game/brawl_icon>"),
    logo = Format.mm("<glyph:game/brawl_logo>"),
    tabLogo = Format.mm("<glyph:game/brawl_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    spawns = BrawlSpawn.entries,
    scoreboard = "brawlScoreboard",
)