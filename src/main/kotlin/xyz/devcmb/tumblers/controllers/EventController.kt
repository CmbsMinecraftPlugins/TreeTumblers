package xyz.devcmb.tumblers.controllers

import kotlinx.coroutines.launch
import net.kyori.adventure.audience.Audience
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.ControllerDelegate
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.annotations.Configurable
import xyz.devcmb.tumblers.annotations.Controller
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.ui.UserInterfaceUtility
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.MiscUtils
import xyz.devcmb.tumblers.util.tumblingPlayer

@Controller("eventController", Controller.Priority.MEDIUM)
class EventController : IController {
    lateinit var teamScores: HashMap<Team, Int>
    private val databaseController: DatabaseController by lazy {
        ControllerDelegate.getController("databaseController") as DatabaseController
    }
    lateinit var topbarRunnable: BukkitRunnable

    companion object {
        @field:Configurable("event.event_mode")
        var eventMode: Boolean = false
    }

    override fun init() {
        TreeTumblers.pluginScope.launch {
            teamScores = databaseController.getTeamScores()
        }

        topbarRunnable = object : BukkitRunnable() {
            override fun run() = sendDefaultTopbar()
        }
        topbarRunnable.runTaskTimer(TreeTumblers.plugin, 0, 20)
    }

    fun sendDefaultTopbar() {
        var teamComponent = Component.empty()

        getEventTeamPlacements().forEachIndexed { i, it ->
            var playersComponent = Component.text(" ")
            databaseController.whitelistedPlayersCache.toSortedMap().filter { entry -> entry.value == it.first }.toList().forEachIndexed { index, entry ->
                val uuid = databaseController.whitelistedPlayerUUIDs[entry.first]!!
                val onlinePlayer = Bukkit.getPlayer(uuid)
                var name = Component.empty()
                if(index != 0) {
                    name = name.append(Component.text(" • ", NamedTextColor.WHITE))
                }

                name = name.append(Format.mm("<color:${if(onlinePlayer != null) "white" else "dark_gray"}><head:$uuid></color> ")).append(
                    if(onlinePlayer != null) Component.text(onlinePlayer.name, it.first.color)
                    else Component.text(entry.first, NamedTextColor.GRAY)
                )

                playersComponent = playersComponent.append(name)
            }
            playersComponent = playersComponent.append(Component.text(" "))

            teamComponent = teamComponent.append(
                Format.mm(
                    " <br><white>#${it.second}</white> <team><shift>${" ".repeat(60)}<gold>${teamScores[it.first]!!}</gold> <br><players><br>",
                    Placeholder.component("team", it.first.formattedName),
                    // By negative spacing, I don't need to do any math for the repetition (and don't need to use periods)
                    Placeholder.component("shift", UserInterfaceUtility.negativeSpace(UserInterfaceUtility.getPixelWidth(it.first.teamName) + 11)),
                    Placeholder.component("players", playersComponent)
                )
            )
        }

        Audience.audience(Bukkit.getOnlinePlayers()).sendPlayerListHeader(
            Component.empty()
                .appendNewline()
                .append(Component.text("\u0001").font(UserInterfaceUtility.ICONS))
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .appendNewline()
                .append(
                    Component.text("EVENT MODE: ", NamedTextColor.AQUA)
                        .append(Component.text(
                            if(eventMode) "ENABLED" else "DISABLED",
                            if(eventMode) NamedTextColor.GREEN else NamedTextColor.RED
                        ))
                )
                .appendNewline()
                .append(teamComponent)
        )
    }

    fun grantScore(player: Player, amount: Int) {
        if(!eventMode) return
        val tumblingPlayer = player.tumblingPlayer

        tumblingPlayer.score += amount
        teamScores.put(tumblingPlayer.team, (teamScores[tumblingPlayer.team] ?: 0) + amount)
    }

    fun grantTeamScore(team: Team, amount: Int) {
        if(!eventMode) return
        teamScores.put(team, (teamScores[team] ?: 0) + amount)
    }

    fun getEventTeamPlacements(): ArrayList<Pair<Team, Int>> {
        val sorted = teamScores.entries.sortedWith(
            compareByDescending<MutableMap.MutableEntry<Team, Int>> { it.value }
                .thenBy { it.key.priority }
        )
        return MiscUtils.calculatePlacements(sorted)
    }

    override fun cleanup() {
        if(!eventMode) return
        TreeTumblers.pluginScope.launch {
            databaseController.replicateTeamData(teamScores)
        }
    }
}