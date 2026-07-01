package xyz.devcmb.tumblers.engine.score

enum class CommonScoreSource(override val id: String) : ScoreSource {
    KILL("kill"),
    OUTLAST("outlast"),
    TEAM_ROUND_WIN("team_round_win"),
    TEAM_ROUND_DRAW("team_round_draw"),
    TEAM_ROUND_LOSE("team_round_lose"),
}