package xyz.devcmb.tumblers.engine.score

enum class CommonScoreSource(override val id: String) : ScoreSource {
    KILL("kill"),
    OUTLAST("outlast"),
    TEAM_PLACEMENT("team_placement"),
    INDIVIDUAL_PLACEMENT("individual_placement"),
    TEAM_ROUND_WIN("team_round_win"),
    TEAM_ROUND_DRAW("team_round_draw"),
    TEAM_ROUND_LOSE("team_round_lose"),
    INDIV_ROUND_WIN("indiv_round_win"),
    INDIV_ROUND_DRAW("indiv_round_draw"),
    INDIV_ROUND_LOSE("indiv_round_lose")
}