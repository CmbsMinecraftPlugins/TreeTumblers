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
        const val TEAM_SCOREBOARD_PLACEMENT = "<placement>. <team> - <gold><score></gold>"

        /**
         * placement - The player's placement
         *
         * head - The player's head component
         *
         * name - The player's [xyz.devcmb.tumblers.util.Format.formatPlayerName]
         *
         * score - The player's score
         */
        const val INDIVIDUAL_SCOREBOARD_PLACEMENT = "<placement>. <head> <name> - <gold><score></gold>"

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

        /**
         * player - The player who died
         */
        val DEATH_MESSAGES = arrayListOf(
            "<player> tripped",
            "<player> didn't try hard enough",
            "<player> thought they could speed bridge",
            "<player> had a skill issue",
            "<player> became a pork chop",
            "<player> went kaboom",
            "<player> was caught playing fortnite",
            "<player> should get their eyes checked",
            "<player> should buy a better pc",
            "<player> got banned from discord"
        ).map { "<gray>$it</gray>" }
    }

    object Event {
        const val EVENT_SCOREBOARD_TITLE = "<green><b>Tree Tumblers</b></green>"
    }
}