package xyz.devcmb.tumblers.controllers

import org.bukkit.configuration.file.FileConfiguration
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.util.DebugUtil
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/*
 * Database documentation time
 *
 * TEAM (tumbling_teams):
 * name - Relational to the Team.teamName field
 * players - A set of all UUIDs of players on the team
 * score - Whole team score
 *
 * PLAYER (tumbling_players):
 * uuid - Player's UUID
 * username - The player's username at the time of being whitelisted
 * score - Player's individual score
 */

@Controller("databaseController", Controller.Priority.HIGH)
class DatabaseController : IController {
    companion object {
        @field:Configurable("database.host", true)
        var host: String = ""

        @field:Configurable("database.port")
        var port: Int = 3306

        @field:Configurable("database.username", true)
        var username: String = ""

        @field:Configurable("database.password", true)
        var password: String = ""

        @field:Configurable("database.database")
        var database: String = ""
    }

    lateinit var connection: Connection
    override fun init() {
        val url = "jdbc:mysql://$host:$port/$database?useSSL=false"

        try {
            connection = DriverManager.getConnection(url, username, password)
            DebugUtil.success("Successfully connected to the MySQL database.")

            createTables()
        } catch (e: SQLException) {
            DebugUtil.severe("Failed to connect to the MySQL database: ${e.message}")
        }
    }

    private fun createTables() {
        val createPlayers = """
            CREATE TABLE IF NOT EXISTS `tumbling_players` (
              `uuid` TEXT NOT NULL,
              `score` TEXT NOT NULL,
              `username` TEXT NOT NULL,
              PRIMARY KEY (`uuid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        """.trimIndent()

        val createTeams = """
            CREATE TABLE IF NOT EXISTS `tumbling_teams` (
                `name` TEXT NOT NULL,
                `players` TEXT NOT NULL,
                `score` INT NOT NULL
            )
        """.trimIndent()

        try {
            connection.createStatement().use {
                it.executeUpdate(createPlayers)
                it.executeUpdate(createTeams)
            }
        } catch (e: SQLException) {
            DebugUtil.severe("Failed to create default tables in the MySQL database: ${e.message}")
        }
    }
}