package xyz.devcmb.tumblers.controllers

import com.destroystokyo.paper.profile.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.DebugUtil
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.util.UUID

/*
 * Database documentation time
 *
 * TEAM (tumbling_teams):
 * name - Relational to the Team enum name to represent its id
 * score - Whole team score
 *
 * PLAYER (tumbling_players):
 * uuid - Player's UUID
 * username - The player's username at the time of being whitelisted
 * score - Player's individual score
 * team - The enum name of the team the player is on
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
    private val eventController: EventController by lazy {
        ControllerDelegate.getController("eventController") as EventController
    }

    override fun init() {
        val url = "jdbc:mysql://$host:$port/$database?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"

        // we can block here because it's before the server loads
        runBlocking {
            try {
                // apparently this is needed to force the driver to load
                Class.forName("com.mysql.jdbc.Driver")

                connection = DriverManager.getConnection(url, username, password)
                DebugUtil.success("Successfully connected to the MySQL database.")

                createTables()
                setupTeams()
                getWhitelistedPlayers()
            } catch (e: SQLException) {
                DebugUtil.severe("Failed to connect to the MySQL database: ${e.message}")
            }
        }
    }

    private suspend fun createTables() = withContext(Dispatchers.IO) {
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

    private suspend fun setupTeams() = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            INSERT IGNORE INTO tumbling_teams (name, score)
            VALUES (?, 0)
        """.trimIndent())

        Team.entries.filter { it.playingTeam }.forEach { entry ->
            statement.setString(1, entry.name.lowercase())
            statement.addBatch()
        }

        statement.executeBatch()
    }

    val whitelistedPlayersCache: HashMap<String, Team> = HashMap()
    val whitelistedPlayerUUIDs: HashMap<String, UUID> = HashMap()
    var hasCached: Boolean = false

    suspend fun whitelistPlayer(profile: PlayerProfile, team: Team) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(
            """
                INSERT INTO tumbling_players (uuid, username, team, score, whitelisted) 
                    VALUES (?, ?, ?, 0, true)
                    ON DUPLICATE KEY UPDATE
                        whitelisted = VALUES(whitelisted),
                        team = VALUES(team)
            """.trimIndent()
        )
        statement.setString(1, profile.id!!.toString())
        statement.setString(2, profile.name)
        statement.setString(3, team.name.lowercase())

        statement.executeUpdate()
        whitelistedPlayersCache.put(profile.name!!, team)
        whitelistedPlayerUUIDs.put(profile.name!!, profile.id!!)
    }

    suspend fun unwhitelistPlayer(profile: PlayerProfile) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement(
            """
                UPDATE tumbling_players
                SET whitelisted = false
                WHERE uuid = ?
            """.trimIndent()
        )

        statement.setString(1, profile.id.toString())

        statement.executeUpdate()
        whitelistedPlayersCache.remove(profile.name)
        whitelistedPlayerUUIDs.remove(profile.name)
    }

    suspend fun replicatePlayerData(player: TumblingPlayer) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            UPDATE tumbling_players
            SET score = ?
            WHERE uuid = ?
        """.trimIndent())

        statement.setInt(1, player.score)
        statement.setString(2, player.bukkitPlayer.uniqueId.toString())

        statement.executeUpdate()
    }

    suspend fun isWhitelisted(uuid: String): Boolean = withContext(Dispatchers.IO) {
        if(!::connection.isInitialized) return@withContext false

        try {
            val statement = connection.prepareStatement("""
                SELECT * FROM tumbling_players WHERE uuid = ? LIMIT 1;
            """.trimIndent())

            statement.setString(1, uuid)

            val resultSet = statement.executeQuery()
            if(resultSet.next()) {
                return@withContext resultSet.getBoolean("whitelisted")
            }

            false
        } catch(e: Exception) {
            DebugUtil.severe("Failed to check if player is whitelisted. Failing closed for $uuid")
            false
        }
    }

    suspend fun getPlayerData(player: Player): TumblingPlayer = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_players WHERE uuid = ? LIMIT 1;
        """.trimIndent())

        statement.setString(1, player.uniqueId.toString())

        val resultSet = statement.executeQuery()
        if(resultSet.next()) {
            val teamColumn: String = resultSet.getString("team")
            val score: Int = resultSet.getInt("score")

            val team = Team.entries.find { it.name.lowercase() == teamColumn.lowercase() }
            if(team == null) {
                throw IllegalStateException("Could not find a team with value $teamColumn")
            }

            TumblingPlayer(player, team, score)
        } else {
            throw IllegalStateException("Could not find player data for ${player.name}")
        }
    }

    suspend fun getWhitelistedPlayers(): HashMap<String, Team> = withContext(Dispatchers.IO) {
        if(!whitelistedPlayersCache.isEmpty() && hasCached) return@withContext whitelistedPlayersCache

        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_players WHERE whitelisted = true;
        """.trimIndent())

        val names: HashMap<String, Team> = HashMap()
        val uuids: HashMap<String, UUID> = HashMap()
        val resultSet = statement.executeQuery()
        while(resultSet.next()) {
            names.put(
                resultSet.getString("username"),
                Team.entries.find { it.name.lowercase() == resultSet.getString("team").lowercase() }!!
            )

            uuids.put(
                resultSet.getString("username"),
                UUID.fromString(resultSet.getString("uuid"))
            )
        }

        whitelistedPlayersCache.putAll(names)
        whitelistedPlayerUUIDs.putAll(uuids)
        hasCached = true

        names
    }

    suspend fun setPlayerTeam(profile: PlayerProfile, team: Team) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            UPDATE tumbling_players
            SET team = ?
            WHERE uuid = ?
        """.trimIndent())

        statement.setString(1, team.name.lowercase())
        statement.setString(2, profile.id.toString())

        // trying to account for if they change their username
        whitelistedPlayersCache.put(
            whitelistedPlayerUUIDs.filterValues { it == profile.id }.keys.first(),
            team
        )

        try {
            statement.executeUpdate()
        } catch(e: SQLException) {
            DebugUtil.severe("Failed to set player team: ${e.message}")
        }
    }

    suspend fun getTeamScores(): HashMap<Team, Int> = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_teams
        """.trimIndent())

        val resultSet = statement.executeQuery()
        val map: HashMap<Team, Int> = HashMap()
        while(resultSet.next()) {
            val name = resultSet.getString("name")
            val score = resultSet.getInt("score")
            map.put(Team.entries.find { it.name == name.uppercase() }!!, score)
        }

        map
    }

    suspend fun replicateTeamData(scores: HashMap<Team, Int>): Unit = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            UPDATE tumbling_teams
            SET score = ?
            WHERE name = ?
        """.trimIndent())

        Team.entries.filter { it.playingTeam }.forEach { entry ->
            statement.setInt(1, scores[entry]!!)
            statement.setString(2, entry.name.lowercase())
            statement.addBatch()
        }

        statement.executeBatch()
    }

    data class WhitelistedPlayer(val name: String)
}