package xyz.devcmb.tumblers.controllers.games.brawl

import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.games.brawl.BrawlController.Companion.font
import xyz.devcmb.tumblers.engine.GameData

object BrawlData : GameData(
    id = "brawl",
    name = "Brawl",
    votable = true,
    maps = setOf(),
    cutsceneSteps = arrayListOf(),
    flags = setOf(),
    scores = hashMapOf(),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    tabLogo = Component.text("\uEA02").font(font)
        .shadowColor(ShadowColor.none()),
    scoreboard = "brawlScoreboard"
)