package xyz.devcmb.tumblers.controllers.games.tower_ascent

import xyz.devcmb.tumblers.engine.GameData

object TowerAscentData : GameData(
    id = "tower_ascent",
    name = "Tower Ascent",
    votable = true,
    maps = setOf(),
    cutsceneSteps = arrayListOf(),
    flags = setOf(),
    scores = hashMapOf(),
    scoreboard = TowerAscentScoreboard::class
)