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
 * whitelisted - If the player is currently whitelisted
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
                `whitelisted` BOOLEAN NOT NULL,
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

    val whitelistedPlayersCache: MutableSet<String> = HashSet()
    var hasCached: Boolean = false

    fun whitelistPlayer(profile: PlayerProfile, team: Team, onSuccess: () -> Unit, onError: (err: String) -> Unit) {
        val statement = connection.prepareStatement(
            """
                INSERT INTO tumbling_players (uuid, username, team, score, whitelisted) 
                    VALUES (?, ?, ?, 0, true)
                    ON DUPLICATE KEY UPDATE
                        whitelisted = VALUES(whitelisted)
            """.trimIndent()
        )
        statement.setString(1, profile.id.toString())
        statement.setString(2, profile.name)
        statement.setString(3, team.name)

        try {
            statement.executeUpdate()
            whitelistedPlayersCache.add(profile.name!!)
            onSuccess()
        } catch(e: SQLException) {
            DebugUtil.severe("Failed to whitelist player ${profile.name}: ${e.message}")
            onError(e.message ?: "Unknown error")
        }
    }

    fun unwhitelistPlayer(profile: PlayerProfile, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val statement = connection.prepareStatement(
            """
                UPDATE tumbling_players
                SET whitelisted = false
                WHERE uuid = ?
            """.trimIndent()
        )

        statement.setString(1, profile.id.toString())

        try {
            statement.executeUpdate()
            whitelistedPlayersCache.remove(profile.name)
            onSuccess()
        } catch(e: SQLException) {
            DebugUtil.severe("Failed to remove player ${profile.name} from the whitelist: ${e.message}")
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
            SELECT * FROM tumbling_players WHERE uuid = ? LIMIT 1;
        """.trimIndent())

        statement.setString(1, uuid)

        val resultSet = statement.executeQuery()
        if(resultSet.next()) {
            return resultSet.getBoolean("whitelisted")
        }

        return false
    }

    fun getPlayerData(player: Player): TumblingPlayer {
        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_players WHERE uuid = ? LIMIT 1;
        """.trimIndent())

        statement.setString(1, player.uniqueId.toString())

        val resultSet = statement.executeQuery()
        if(resultSet.next()) {
            val teamColumn: String = resultSet.getString("team")
            val score: Int = resultSet.getInt("score")

            val team = Team.values().find { it.name == teamColumn }
            if(team == null) {
                throw IllegalStateException("Could not find a team with value $teamColumn")
            }

            return TumblingPlayer(player, team, score)
        } else {
            throw IllegalStateException("Could not find player data for ${player.name}")
        }
    }

    fun getWhitelistedPlayerNames(): Set<String> {
        if(!whitelistedPlayersCache.isEmpty() && hasCached) return whitelistedPlayersCache

        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_players WHERE whitelisted = true;
        """.trimIndent())

        val names: MutableSet<String> = HashSet()
        val resultSet = statement.executeQuery()
        while(resultSet.next()) {
            names.add(resultSet.getString("username"))
        }

        whitelistedPlayersCache.addAll(names)
        hasCached = true
        return names
    }

    data class WhitelistedPlayer(val name: String)
}