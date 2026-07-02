package xyz.devcmb.tumblers.controllers.games.deathrun

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import org.bukkit.Color
import org.bukkit.Material
import org.bukkit.entity.ArmorStand
import org.bukkit.inventory.ItemStack
import org.bukkit.inventory.meta.ColorableArmorMeta
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController.Companion.lives
import xyz.devcmb.tumblers.controllers.games.deathrun.DeathrunController.DeathrunScoreSource
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format
import xyz.devcmb.tumblers.util.suspendSync
import xyz.devcmb.tumblers.util.validateLocation
import kotlin.collections.forEach

object DeathrunData : GameData(
    id = "deathrun",
    name = "Deathrun",
    votable = false,
    maps = setOf(
        Map("forest")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Format.mm("<glyph:game/deathrun_icon>"))
            .append(Component.text(" Deathrun"))
        ) {
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, 1 team at a time will be the <red>trappers</red>, while everyone else is a <aqua>runner</aqua>"),
            "cutscene.trapper_showcase"
        ) { map ->
            val armorStands: ArrayList<ArmorStand> = ArrayList()
            map.data.getList("cutscene.armor_stands")?.forEach {
                if(it !is List<*>) throw GameControllerException("Cutscene armor stand location table is not a list")
                val location = it.validateLocation(map.world) ?: throw GameControllerException("Cutscene armor stand locations are not valid")

                suspendSync {
                    map.world.spawn(location, ArmorStand::class.java) { stand ->
                        stand.equipment.setHelmet(ItemStack.of(Material.LEATHER_HELMET).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setChestplate(ItemStack.of(Material.LEATHER_CHESTPLATE).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setLeggings(ItemStack.of(Material.LEATHER_LEGGINGS).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        stand.equipment.setBoots(ItemStack.of(Material.LEATHER_BOOTS).apply {
                            itemMeta = (itemMeta as ColorableArmorMeta).also { meta ->
                                meta.setColor(Color.fromRGB(Team.RED.color.value()))
                            }
                        }, true)

                        armorStands.add(stand)
                    }
                }

                delay(500)
            }

            delay(2500)
            suspendSync {
                armorStands.forEach(ArmorStand::remove)
                armorStands.clear()
            }
        },
        CutsceneStep(
            Format.mm("<red>Trappers</red> can activate traps that make progressing harder.<newline><red>Trappers</red> get points when they <yellow>damage</yellow> and when they <red>kill</red> players.<newline><aqua>Runners</aqua> have $lives lives, losing 1 whenever they take damage."),
            "cutscene.trap_showcase"
        ) {
            val trap = (game as DeathrunController).mapTraps[0]!![0]
            delay(1000)
            trap.activate()
            delay(700)
        },
        CutsceneStep(
            Format.mm("The amount of score <aqua>runners</aqua> get from completing a run is based on their <yellow>placement</yellow><newline>The faster they complete the course, the more <yellow>score</yellow> they'll get"),
            "cutscene.runner_score_showcase"
        ) {
            val game = game as DeathrunController
            suspendSync {
                game.summonScoreDisplay()
            }
            delay(6000)
            suspendSync {
                game.endDisplay?.remove()
                game.endDisplay = null
            }
        },
        CutsceneStep.GLHF
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_PVP,
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION,
        Flag.USE_SPECTATOR_DEATH_SYSTEM
    ),
    scores = hashMapOf(
        // this * placement = awarded score
        DeathrunScoreSource.RUN_COMPLETE to 8,
        // constant value
        DeathrunScoreSource.RUN_FAILED to 15,

        // split across the whole team
        DeathrunScoreSource.TRAP_KILL to 40,
        DeathrunScoreSource.TRAP_DAMAGE to 20
    ),
    icon = Format.mm("<glyph:game/deathrun_icon>"),
    logo = Format.mm("<glyph:game/deathrun_logo>"),
    tabLogo = Format.mm("<glyph:game/deathrun_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    scoreboard = "deathrunScoreboard"
)