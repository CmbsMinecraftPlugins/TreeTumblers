package xyz.devcmb.tumblers.controllers.games.sniffer_caretaker

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.Material
import org.bukkit.Sound
import org.bukkit.block.data.Levelled
import org.bukkit.entity.Entity
import org.bukkit.entity.EntityType
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.controllers.games.sniffer_caretaker.SnifferCaretakerController.SnifferCaretakerScoreSource
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.validateLocation

object SnifferCaretakerData : GameData(
    id = "sniffer_caretaker",
    votable = true,
    maps = setOf(
        Map("facility")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/sniffer_caretaker_icon>"))
                .append(Component.text(" Sniffer Caretaker")),
            "cutscene.start"
        ) { _ ->
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, you need to fulfill your <red>sniffer's</red> wants as seen on the <blue>task board.</blue> <blue>Tasks</blue> will give more <yellow>score</yellow> based on how many <aqua>stars</aqua> it has."),
            "cutscene.tasks"
        ) { _ ->
            val game = game as SnifferCaretakerController

            val exampleTasks: List<String> = listOf(
                "hungry_wheat",
                "bored_moss_block",
                "lonely_cow",
                "bored_glass",
                "hungry_cake"
            )

            exampleTasks.forEach {
                suspendSync {
                    game.createNewTask(Team.RED, it, true)
                }
                delay(750)
            }

            delay(3000)
        },
        CutsceneStep(
            Format.mm("<blue>Tasks</blue> range from feeding the sniffer various foods..."),
            "cutscene.farm"
        ) { map ->
            val game = game as SnifferCaretakerController

            suspendSync {
                game.currentTasks[Team.RED]!!.forEach {
                    game.completeTask(Team.RED, it, true)
                }

                game.currentTasks[Team.RED]!!.clear()
            }

            map.data.getList("cutscene.farm_wheat")?.forEach {
                if(it !is List<*>) throw GameControllerException("Cutscene farm wheat location table is not a list")
                val location = it.validateLocation(map.world) ?: throw GameControllerException("Cutscene farm wheat locations are not valid")

                suspendSync {
                    location.block.type = Material.WHEAT
                    map.world.playSound(location, Sound.ITEM_CROP_PLANT, 1.0f, 1.0f)
                }

                delay(100)
            }

            delay(1500)

            suspendSync {
                map.data.getList("cutscene.farm_wheat")?.forEach {
                    if (it !is List<*>) throw GameControllerException("Cutscene farm wheat location table is not a list")
                    val location = it.validateLocation(map.world)
                        ?: throw GameControllerException("Cutscene farm wheat locations are not valid")

                    location.block.type = Material.AIR
                }
            }
        },
        CutsceneStep(
            Format.mm("To giving the sniffer things to sniff..."),
            "cutscene.blocks"
        ) { _ ->
            val game = game as SnifferCaretakerController

            suspendSync {
                game.stockBlocks(Team.RED)
            }

            delay(2000)
        },
        CutsceneStep(
            Format.mm("To quenching the sniffer's thirst..."),
            "cutscene.thirst"
        ) { map ->
            delay(500)

            val cauldron = getLocation("cutscene.thirst_cauldron")
            suspendSync {
                cauldron.block.type = Material.WATER_CAULDRON
                cauldron.block.blockData = (cauldron.block.blockData as Levelled).also {
                    it.level = it.maximumLevel
                }
                map.world.playSound(cauldron, Sound.ITEM_BUCKET_EMPTY, 1.0f, 1.0f)
            }

            delay(1500)

            suspendSync {
                cauldron.block.type = Material.CAULDRON
            }
        },
        CutsceneStep(
            Format.mm("To bringing it a friend!"),
            "cutscene.mobs"
        ) { map ->
            val cowLocation = getLocation("cutscene.mobs_cow")
            val chickenLocation = getLocation("cutscene.mobs_chicken")
            lateinit var cow: Entity
            lateinit var chicken: Entity

            suspendSync {
                cow = map.world.spawnEntity(cowLocation, EntityType.COW)
                chicken = map.world.spawnEntity(chickenLocation, EntityType.CHICKEN)
            }

            delay(2500)

            suspendSync {
                cow.remove()
                chicken.remove()
            }
        },
        CutsceneStep(
            Format.mm("The <red>sniffer</red> may also have some rather <i>odd</i> desires, which can be found in the back of the facility in the <red>basement...</red>"),
            "cutscene.basement"
        ) { map ->
            val spiderLocation = getLocation("cutscene.basement_spider")
            lateinit var spider: Entity

            suspendSync {
                spider = map.world.spawnEntity(spiderLocation, EntityType.SPIDER)
            }

            delay(5000)

            suspendSync {
                spider.remove()
            }
        },
        CutsceneStep(
            Format.mm("Remember, your only goal is to keep the <red>sniffer</red> <yellow>happy</yellow>, and complete as many <blue>tasks</blue> as you can!"),
            "cutscene.end"
        ) { _ ->
            delay(4000)
        },
        CutsceneStep.GLHF
    ),
    flags = setOf(Flag.SURVIVAL_MODE),
    scores = hashMapOf(
        SnifferCaretakerScoreSource.TASK_1_STAR to 20,
        SnifferCaretakerScoreSource.TASK_2_STAR to 40,
        SnifferCaretakerScoreSource.TASK_3_STAR to 60,
        SnifferCaretakerScoreSource.TASK_4_STAR to 80,
        SnifferCaretakerScoreSource.TASK_5_STAR to 120
    ),
    icon = Format.mm("<glyph:game/sniffer_caretaker_icon>"),
    logo = Format.mm("<glyph:game/sniffer_caretaker_logo>"),
    tabLogo = Format.mm("<glyph:game/sniffer_caretaker_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    scoreboard = "snifferCaretakerScoreboard",
    name = "Sniffer Caretaker"
)