package xyz.devcmb.tumblers.controllers

import com.destroystokyo.paper.profile.PlayerProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import org.bukkit.Bukkit
import org.bukkit.event.EventHandler
import org.bukkit.event.EventPriority
import org.bukkit.event.player.PlayerJoinEvent
import xyz.devcmb.tumblers.TumblingDatabaseException
import xyz.devcmb.tumblers.TumblingDatabaseStateException
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.controllers.event.BadgeController
import xyz.devcmb.tumblers.controllers.event.EventController
import xyz.devcmb.tumblers.controllers.event.VotingController
import xyz.devcmb.tumblers.controllers.games.GameController
import xyz.devcmb.tumblers.controllers.player.PlayerController
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.data.TumblingPlayer
import xyz.devcmb.tumblers.util.DebugUtil
import java.sql.Connection
import java.sql.DriverManager
import java.sql.SQLException
import java.sql.Timestamp
import java.util.Date
import java.util.UUID
import kotlin.toString

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

@Controller(Controller.Priority.HIGH)
class DatabaseController : ControllerBase() {
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
        private set

    private val eventController: EventController by controller()
    private val playerController: PlayerController by controller()
    private val gameController: GameController by controller()
    private val votingController: VotingController by controller()

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
                loadRecoveryStates()
            } catch (e: SQLException) {
                DebugUtil.severe("Failed to connect to the MySQL database: ${e.message}")
            }
        }
    }

    fun isConnected(): Boolean = ::connection.isInitialized

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

        val createBadges = """
            CREATE TABLE IF NOT EXISTS `tumbling_badges` (
                `badge` VARCHAR(255) NOT NULL,
                `game` VARCHAR(255) NOT NULL,
                `player` VARCHAR(255) NOT NULL COMMENT "The UUID of the player who completed the badge",
                `achieved` TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_general_ci;
        """.trimIndent()

        val createState = """
            CREATE TABLE IF NOT EXISTS `tumbling_state` (
                id VARCHAR(12) PRIMARY KEY,
                state JSON NOT NULL,
                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
            )
        """.trimIndent()

        try {
            connection.createStatement().use {
                it.executeUpdate(createPlayers)
                it.executeUpdate(createTeams)
                it.executeUpdate(createBadges)
                it.executeUpdate(createState)
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

    suspend fun whitelistPlayer(profile: PlayerProfile, team: Team) = withContext(Dispatchers.IO) {
        require(profile.id != null) { "PlayerProfile does not have a UUID" }

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

        val scoreStatement = connection.prepareStatement("""
            SELECT score FROM tumbling_players WHERE uuid = ?
        """.trimIndent())

        scoreStatement.setString(1, profile.id.toString())

        val score = scoreStatement.executeQuery()
        if(!score.next()) throw TumblingDatabaseException("Score not found in database directly after update")

        playerController.registerTumblingPlayer(profile.id!!, profile.name!!, team, score.getInt("score"))
    }

    suspend fun unwhitelistPlayer(profile: PlayerProfile) = withContext(Dispatchers.IO) {
        require(profile.id != null) { "PlayerProfile does not have a UUID" }

        val statement = connection.prepareStatement(
            """
                UPDATE tumbling_players
                SET whitelisted = false
                WHERE uuid = ?
            """.trimIndent()
        )

        statement.setString(1, profile.id.toString())

        playerController.unregisterTumblingPlayer(profile.id!!)
        statement.executeUpdate()
    }

    suspend fun replicatePlayerData(player: TumblingPlayer) = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            UPDATE tumbling_players
            SET score = ?, team = ?
            WHERE uuid = ?
        """.trimIndent())

        statement.setInt(1, player.score)
        statement.setString(2, player.team.name.lowercase())
        statement.setString(3, player.uuid.toString())

        statement.executeUpdate()

        player.badges.forEach { badge, timestamp ->
            val insertStatement = connection.prepareStatement("""
                INSERT INTO tumbling_badges (badge, game, player, achieved)
                SELECT ?, ?, ?, ?
                WHERE NOT EXISTS (
                    SELECT 1 FROM tumbling_badges 
                    WHERE badge = ? AND player = ?
                )
            """.trimIndent())

            insertStatement.setString(1, badge.name.lowercase())
            insertStatement.setString(2, badge.game)
            insertStatement.setString(3, player.uuid.toString())
            insertStatement.setTimestamp(4, timestamp)
            insertStatement.setString(5, badge.name.lowercase())
            insertStatement.setString(6, player.uuid.toString())

            insertStatement.executeUpdate()
        }
    }

    fun isWhitelisted(uuid: String): Boolean = playerController.players.any { it.uuid == UUID.fromString(uuid) }

    suspend fun getAllPlayerData(): ArrayList<TumblingPlayer> = withContext(Dispatchers.IO) {
        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_players WHERE whitelisted = true;
        """.trimIndent())

        val tumblingPlayers: ArrayList<TumblingPlayer> = ArrayList()
        val resultSet = statement.executeQuery()
        while(resultSet.next()) {
            val teamColumn: String = resultSet.getString("team")
            val score: Int = resultSet.getInt("score")
            val uuidColumn = resultSet.getString("uuid")
            val username = resultSet.getString("username")

            val team = Team.entries.find { it.name.lowercase() == teamColumn.lowercase() }
            if(team == null) {
                throw TumblingDatabaseException("Could not find a team with value $teamColumn")
            }

            val uuid = UUID.fromString(uuidColumn)
            val tumblingPlayer = TumblingPlayer(uuid)

            val badgesStatement = connection.prepareStatement("""
                SELECT * FROM tumbling_badges WHERE player = ?
            """.trimIndent())
            badgesStatement.setString(1, uuidColumn)

            val result = badgesStatement.executeQuery()
            val badges = HashMap<BadgeController.Badge, Timestamp>()
            while(result.next()) {
                val id = result.getString("badge")
                val game = result.getString("game")
                val timestamp = result.getTimestamp("achieved")

                val registeredGame = gameController.games.find { it.id == game }
                    ?: throw TumblingDatabaseStateException("Could not find a game with id $game")

                // don't need to throw here because badges could technically get removed (shouldn't, but can)
                // if a game gets fully removed and had badges we have bigger fish to fry
                val badge = registeredGame.badges?.find { it.name.lowercase() == id } ?: continue

                badges.put(badge, timestamp)
            }

            tumblingPlayer.bukkitPlayer = Bukkit.getPlayer(uuid)
            tumblingPlayer.score = score
            tumblingPlayer.name = username
            tumblingPlayer.team = team
            tumblingPlayer.badges.putAll(badges)

            tumblingPlayers.add(tumblingPlayer)
        }

        tumblingPlayers
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

    val recoveryStates: ArrayList<EventRecoveryState> = ArrayList()
    suspend fun saveEventState() = withContext(Dispatchers.IO) {
        val state = EventController.EventState(
            eventController.state != EventController.State.EVENT_INACTIVE,
            eventController.game,
            HashMap(votingController.quadrantGames.map { it.key to it.value.id }.toMap()),
            eventController.playedGames,
            eventController.lastGameTeamPlacements,
            eventController.lastGamePlayerPlacements?.map { it.first.uuid.toString() to it.second },
            eventController.lastGameTeamScores,
            eventController.lastGamePlayerScores?.let { HashMap(it.map { entry -> entry.key.uuid.toString() to entry.value }.toMap()) },
            eventController.teamScores,
            HashMap(playerController.players.associate { it.uuid.toString() to it.score })
        )

        val id = UUID.randomUUID().toString().replace("-", "").take(12)
        recoveryStates.add(EventRecoveryState(id, state, Timestamp(Date().time)))

        val statement = connection.prepareStatement("""
            INSERT INTO tumbling_state (id, state) VALUES (?, ?)
        """.trimIndent())

        statement.setString(1, id)
        statement.setString(2, Json.encodeToString(state))
        statement.executeUpdate()
    }

    suspend fun loadRecoveryStates() = withContext(Dispatchers.IO) {
        recoveryStates.clear()

        val statement = connection.prepareStatement("""
            SELECT * FROM tumbling_state
        """.trimIndent())

        val resultSet = statement.executeQuery()
        while(resultSet.next()) {
            val id: String = resultSet.getString("id")
            val stateJson: String = resultSet.getString("state")
            val timestamp: Timestamp = resultSet.getTimestamp("created_at")

            val eventState: EventController.EventState = Json.decodeFromString(stateJson)
            recoveryStates.add(EventRecoveryState(id, eventState, timestamp))
        }
    }

    data class EventRecoveryState(val id: String, val eventState: EventController.EventState, val timestamp: Timestamp)

    @EventHandler(priority = EventPriority.LOWEST)
    fun playerJoin(event: PlayerJoinEvent) {
        val player = event.player
        if(playerController.players.any { it.uuid == player.uniqueId }) return

        val team = Team.entries.filter { it.playingTeam }.random()
        playerController.registerTumblingPlayer(player.uniqueId, player.name, team, 0)
    }
}