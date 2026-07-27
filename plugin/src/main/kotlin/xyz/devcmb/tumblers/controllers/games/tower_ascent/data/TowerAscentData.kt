package xyz.devcmb.tumblers.controllers.games.tower_ascent.data

import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentScoreboard
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map

object TowerAscentData : GameData(
    id = "tower_ascent",
    name = "Tower Ascent",
    votable = true,
    maps = setOf(Map("tower")),
    cutsceneSteps = arrayListOf(
        CutsceneStep.GLHF
    ),
    flags = setOf(
        Flag.ENABLE_HUNGER
    ),
    scores = hashMapOf(
        TowerAscentScoreSource.COMPLETE_ROOM to 120,
        TowerAscentScoreSource.COMPLETE_TOWER to 300,
    ),
    scoreboard = TowerAscentScoreboard::class,
    spawns = TowerAscentSpawn.entries
)