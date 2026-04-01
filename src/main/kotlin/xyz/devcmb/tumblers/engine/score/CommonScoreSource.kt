package xyz.devcmb.tumblers.engine.score

enum class CommonScoreSource(override val id: String) : ScoreSource {
    KILL("kill"),
    OUTLAST("outlast"),
    @Deprecated("Team scores are being phased out, please only use individual scoring")
    TEAM_PLACEMENT("team_placement"),
    @Deprecated("Individual bonuses are being removed")
    INDIVIDUAL_PLACEMENT("individual_placement"),
    TEAM_ROUND_WIN("team_round_win"),
    TEAM_ROUND_DRAW("team_round_draw"),
    TEAM_ROUND_LOSE("team_round_lose"),
}