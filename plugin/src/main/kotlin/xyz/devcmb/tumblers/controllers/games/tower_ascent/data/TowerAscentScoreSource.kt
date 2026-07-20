package xyz.devcmb.tumblers.controllers.games.tower_ascent.data

import xyz.devcmb.tumblers.engine.score.ScoreSource

enum class TowerAscentScoreSource(override val id: String) : ScoreSource {
    COMPLETE_TOWER("tower_ascent_complete_tower"),
    COMPLETE_ROOM("tower_ascent_complete_room"),
}