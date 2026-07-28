package xyz.devcmb.tumblers.controllers.games.tower_ascent.data

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.Material
import org.bukkit.entity.Entity
import org.bukkit.entity.Skeleton
import org.bukkit.entity.Zombie
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentController
import xyz.devcmb.tumblers.controllers.games.tower_ascent.TowerAscentScoreboard
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.isEnclosed
import xyz.devcmb.tumblers.util.randomBetween
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.toCenterXZLocation

object TowerAscentData : GameData(
    id = "tower_ascent",
    name = "Tower Ascent",
    votable = true,
    maps = setOf(Map("tower")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/tower_ascent_icon>"))
                .append(Component.text(" Tower Ascent")),
            "cutscene.start"
        ) { _ ->
            delay(5000)
        },
        CutsceneStep(
            Format.mm("<white>In this game, you have to <yellow>climb the tower</yellow> while killing <red>monsters</red> along the way.</white>"),
            "cutscene.first"
        ) {
            delay(3000)
        },
        CutsceneStep(
            Format.mm("<white>Every <aqua>room</aqua> has a set of mobs you will need to <red>slay</red> before proceeding to the next one.</white>"),
            "cutscene.first"
        ) {
            val towerAscent = game as TowerAscentController
            val viewingRoom = towerAscent.generator.towerHandlers.first().rooms.first()

            val mobs: ArrayList<Entity> = ArrayList()
            suspendSync {
                repeat(10) {
                    val location = viewingRoom.roomBounds.first.randomBetween(viewingRoom.roomBounds.second) { block ->
                        block.type != Material.AIR
                        && block.location.clone().add(0.0,1.0,0.0).block.type == Material.AIR
                        && block.location.clone().add(0.0, 2.0, 0.0).block.type == Material.AIR
                        && block.location.isEnclosed()
                    }!!.add(0.0,1.0,0.0).toCenterXZLocation()

                    val type = if((1..2).random() == 1) Zombie::class.java else Skeleton::class.java
                    game.map.world.spawn(location, type) {
                        it.setAI(false)
                        mobs.add(it)
                    }
                }
            }

            delay(6000)
            suspendSync {
                mobs.forEach { it.remove() }
            }
        },
        CutsceneStep(
            Format.mm("<white>Every monster you <red>slay</red> will give you <gold>gold</gold> <sprite:items:item/gold_ingot> which you can spend on <green>items</green> at the shop.</white>"),
            "cutscene.shop"
        ) {
            delay(6000)
        },
        CutsceneStep(
            Format.mm("<white>However, <color:${Team.ORANGE.color.asHexString()}>be careful!</color> If you <red>die</red> in the tower, you'll lose all your <green>purchased items</green> and all your <gold>gold</gold></white>"),
            "cutscene.shop"
        ) {
            delay(6000)
        },
        CutsceneStep(
            Format.mm("<white>The <green>faster</green> you complete the tower, the <aqua>more score</aqua> you'll earn!<br>You'll also earn a <gold>bonus</gold> for any gold you keep until the end of the run."),
            "cutscene.end"
        ) {
            delay(4000)
        },
        CutsceneStep.GLHF
    ),
    flags = setOf(
        Flag.ENABLE_HUNGER,
        Flag.RESET_MOB_TARGETS_ON_DEATH
    ),
    scores = hashMapOf(
        TowerAscentScoreSource.COMPLETE_ROOM to 120,
        TowerAscentScoreSource.COMPLETE_TOWER to 300,
    ),
    scoreboard = TowerAscentScoreboard::class,
    spawns = TowerAscentSpawn.entries
)