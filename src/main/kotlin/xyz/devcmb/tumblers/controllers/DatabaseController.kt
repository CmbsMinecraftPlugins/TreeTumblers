package xyz.devcmb.tumblers.controllers

import com.destroystokyo.paper.profile.PlayerProfile
import org.bukkit.OfflinePlayer
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.DebugUtil
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException

/*
 * Database documentation time
 *
 * TEAM (tumbling_teams):
 * name - Relational to the Team.teamName field
 * score - Whole team score
 *
 * PLAYER (tumbling_players):
 * uuid - Player's UUID
 * username - The player's username at the time of being whitelisted
 * score - Player's individual score
 * team - The name of the team the player is on
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
                `uuid` VARCHAR(255) NOT NULL,
                `username` TEXT NOT NULL,
                `team` TEXT NOT NULL,
                `score` TEXT NOT NULL,
                PRIMARY KEY (`uuid`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        """.trimIndent()

        val createTeams = """
            CREATE TABLE IF NOT EXISTS `tumbling_teams` (
                `name` VARCHAR(255) NOT NULL,
                `score` INT NOT NULL,
                PRIMARY KEY (`name`)
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
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

    fun whitelistPlayer(profile: PlayerProfile, team: Team, onSuccess: () -> Unit, onError: (err: String) -> Unit) {
        val statement = connection.prepareStatement(
            "INSERT INTO tumbling_players VALUES (?, ?, ?, 0)"
        )
        statement.setString(1, profile.id.toString())
        statement.setString(2, profile.name)
        statement.setString(3, team.name)

        try {
            statement.executeUpdate()
            onSuccess()
        } catch(e: SQLException) {
            DebugUtil.severe("Failed to whitelist player ${profile.name}: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }

    fun replicatePlayerData(player: TumblingPlayer) {
        val statement = connection.prepareStatement("""
            UPDATE tumbling_players
            SET score = ?, team = ?
            WHERE uuid = ?
        """.trimIndent())

        statement.setInt(1, player.score)
        statement.setString(2, player.team.name)
        statement.setString(3, player.bukkitPlayer.uniqueId.toString())

        try {
            statement.executeUpdate()
        } catch(e: SQLException) {
            DebugUtil.severe("Failed to replicate player data: ${e.message}")
        }
    }

    fun isWhitelisted(uuid: String): Boolean {
        val statement = connection.prepareStatement("""
            SELECT COUNT(*) FROM tumbling_players WHERE uuid = ?;
        """.trimIndent())

        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if(resultSet.next()) {
            return resultSet.getInt(1) > 0
        }

        return false
    }
}