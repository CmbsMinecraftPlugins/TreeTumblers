package xyz.devcmb.tumblers.controllers.games.breach

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Item
import org.bukkit.inventory.ItemStack
import org.bukkit.scheduler.BukkitRunnable
import xyz.devcmb.tumblers.TreeTumblers
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.runTaskLater
import xyz.devcmb.tumblers.util.suspendSync

object BreachData : GameData(
    id = "breach",
    name = "Breach",
    votable = false,
    maps = setOf(
        Map("stadium")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/breach_icon>"))
                .append(Component.text(" Breach")),
            "cutscene.start"
        ) { _ ->
            delay(5000)
        },
        CutsceneStep(Format.mm("Each round you get to pick a weapon, and one person on the team needs to hold the <light_purple>star.</light_purple>"),
            "cutscene.preround"
        ) { _ ->
            delay(5000)
        },
        CutsceneStep(Format.mm("A round is <green>won</green> by stealing the other team's <light_purple>star</light_purple>, by <red>killing</red> the holder of the <light_purple>star</light_purple> and picking it up before their teammates can retrieve it"),
            "cutscene.star"
        ) { map ->
            delay(1000)
            val starLocation = getLocation("cutscene.star_spawn")
            lateinit var star: Item
            suspendSync {
                star = map.world.dropItem(starLocation, ItemStack.of(Material.NETHER_STAR))
                star.isGlowing = true
            }
            delay(4000)
            suspendSync {
                star.remove()
            }
        },
        CutsceneStep(Format.mm("As the round goes on, walls will start to crack and break down, opening up the map"),
            "cutscene.breaking"
        ) { _ ->
            val game = game as BreachController
            var breakingTask = object : BukkitRunnable() {
                override fun run() {
                    game.breakBlock()
                }
            }

            breakingTask.runTaskTimer(TreeTumblers.plugin, 0, 3)

            runTaskLater(9 * 20) {
                breakingTask.cancel()
            }

            delay(5000)
        },
        CutsceneStep(Format.mm("The first team to win 3 rounds, <green>wins it all.</green>"),
            "cutscene.end"
        ) { _ ->
            delay(4000)
        },
        CutsceneStep(Format.mm("<b><gold>Good Luck, Have Fun, and may the best team win!</gold></b>"),
            "cutscene.end"
        ) { _ ->
            // A comment from "Nibbles"
            // Ts Pmo Why Don It Work Pmo Pmo Pmo Pmo

//            val game = game as BreachController
//            val color = game.playingTeams.first.color
//
//            val team1spawn = getLocation("team_1_spawn").clone()
//            team1spawn.setRotation(0f, 0f)
//
//            suspendSync {
//                DebugUtil.info("$team1spawn")
//                MiscUtils.spawnFirework(team1spawn, FireworkEffect.builder()
//                    .trail(false)
//                    .flicker(true)
//                    .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                    .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                    .with(FireworkEffect.Type.STAR)
//                    .build()
//                )
//            }
            //delay(2000)
        },
//        CutsceneStep(Format.mm("<b><green>Have Fun.</green></b>"),
//            "cutscene.end"
//        ) { map ->
//            val game = game as BreachController
//            val color = game.playingTeams.second.color
//
//            val team2spawn = getLocation("team_2_spawn")
//
//            suspendSync {
//                MiscUtils.spawnFirework(
//                    team2spawn, FireworkEffect.builder()
//                        .trail(false)
//                        .flicker(true)
//                        .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                        .withColor(Color.fromRGB(color.red(), color.green(), color.blue()))
//                        .with(FireworkEffect.Type.STAR)
//                        .build()
//                )
//            }
//            delay(2000)
//        },
    ),
    flags = setOf(
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION,
        Flag.DISABLE_FALL_DAMAGE,
        Flag.HIDE_ENEMY_NAMETAGS,
        Flag.CUSTOM_DEATH_SYSTEM,
        Flag.HIDE_HEALTH_INDICATOR
    ),
    scores = hashMapOf(),
    scoreboard = BreachScoreboard::class
)