package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController.Companion.font
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map

object BrawlData : GameData(
    id = "brawl",
    name = "Brawl",
    votable = true,
    maps = setOf(Map("district")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(font))
                .append(Component.text(" Brawl")),
            "cutscene.start"
        ) { map ->
            delay(5000)
        },
    ),
    flags = setOf(),
    scores = hashMapOf(),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    tabLogo = Component.text("\uEA02").font(font)
        .shadowColor(ShadowColor.none()),
    spawns = BrawlSpawn.entries,
    scoreboard = "brawlScoreboard",
)