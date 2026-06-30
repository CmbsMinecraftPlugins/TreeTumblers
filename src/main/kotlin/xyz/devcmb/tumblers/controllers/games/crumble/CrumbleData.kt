package xyz.devcmb.tumblers.controllers.games.crumble

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.controllers.games.crumble.CrumbleController.Companion.font
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.util.Format
import kotlin.math.sqrt

object CrumbleData : GameData(
    id = "crumble",
    name = "Crumble",
    votable = true,
    maps = setOf(
        Map("warfare")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Component.text("\uEA00").font(NamespacedKey(TreeTumblers.NAMESPACE, "games/crumble")))
                .append(Component.text(" Crumble")),
            "cutscene.start"
        ) { map ->
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, as time goes on, the map will <yellow>crumble away</yellow> in a circle"),
            "cutscene.crumble_demonstration"
        ) { map ->
            val center = getLocation("centers.cutscene")
            val currentMapSize = map.data.getInt("map_size")
            var currentCrumbleRadius = ((currentMapSize * sqrt(2.0) * 0.5) + 1)

            // maybe extract this duplicate code
            val runnable = object : BukkitRunnable() {
                override fun run() {
                    val radiusSquared = currentCrumbleRadius * currentCrumbleRadius

                    val halfMap = currentMapSize / 2
                    val xStart = (center.x - halfMap).toInt()
                    val xEnd = (center.x + halfMap).toInt()
                    val zStart = (center.z - halfMap).toInt()
                    val zEnd = (center.z + halfMap).toInt()
                    val yStart = (center.y - 50).toInt()
                    val yEnd = (center.y + 10).toInt()

                    for (x in xStart..xEnd) {
                        val dx = x + 0.5 - center.x
                        for (z in zStart..zEnd) {
                            val dz = z + 0.5 - center.z
                            val distSq = dx*dx + dz*dz
                            if (distSq < radiusSquared) continue

                            for (y in yStart..yEnd) {
                                val block = center.world.getBlockAt(x, y, z)
                                if (!block.type.isAir) {
                                    block.type = Material.AIR
                                }
                            }
                        }
                    }

                    currentCrumbleRadius -= 0.2
                }
            }
            runnable.runTaskTimer(TreeTumblers.plugin, 0, 1)

            delay(4000)
            runnable.cancel()
        },
        CutsceneStep(
            Format.mm("This game was originally designed by <click:open_url:https://www.youtube.com/@MatMart><u><red>Mat</red><white>Mart</white></u></click>, coded by <click:open_url:https://blackilykat.dev><u><color:#e09cff>Blackilykat</color></u></click>, and funded by <click:open_url:https://www.youtube.com/@Cobgd><color:#ff701e><u>GDCob</u></color></click>!"),
            "cutscene.credit"
        ) { map ->
            delay(4000)
        },
        CutsceneStep.GLHF
    ),
    flags = setOf(
        Flag.SURVIVAL_MODE,
        Flag.USE_SPECTATOR_DEATH_SYSTEM_NO_ACTIONBAR,
        Flag.DISABLE_NATURAL_REGENERATION
    ),
    scores = hashMapOf(
        CommonScoreSource.KILL to 45,
        CommonScoreSource.TEAM_ROUND_WIN to 480,
        CommonScoreSource.TEAM_ROUND_DRAW to 240,
        CommonScoreSource.TEAM_ROUND_LOSE to 120,
    ),
    icon = Component.text("\uEA00").font(font),
    logo = Component.text("\uEA01").font(font)
        .shadowColor(ShadowColor.none()),
    tabLogo = Component.text("\uEA02").font(font)
        .shadowColor(ShadowColor.none()),
    scoreboard = "crumbleScoreboard",
    badges = CrumbleBadge.entries,
    spawns = CrumbleSpawn.entries
)