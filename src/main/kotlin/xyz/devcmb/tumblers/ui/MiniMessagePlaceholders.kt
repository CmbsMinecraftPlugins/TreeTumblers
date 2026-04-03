package xyz.devcmb.tumblers.ui

object MiniMessagePlaceholders {
    object Game {
        /**
         * placement - The team's placement in the game
         *
         * team - The [xyz.devcmb.tumblers.data.Team.formattedName]
         *
         * score - The team's score
         */
        const val TEAM_SCOREBOARD_PLACEMENT = "<placement>. <team> - <score>"

        /**
         * placement - The player's placement
         *
         * head - The player's head component
         *
         * name - The player's [xyz.devcmb.tumblers.util.Format.formatPlayerName]
         *
         * score - The player's score
         */
        const val INDIVIDUAL_SCOREBOARD_PLACEMENT = "<placement>. <head> <name> - <score>"

        /**
         * current - The current round
         *
         * total - The total amount of rounds
         */
        const val SCOREBOARD_CURRENT_ROUND = "<aqua>Round <white><current>/<total></white></aqua>"

        /**
         * name - The name of the game
         */
        const val SCOREBOARD_TITLE = "<yellow><bold><name></bold></yellow>"
    }
}