package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.score.CommonScoreSource
import xyz.devcmb.tumblers.util.Format

object BrawlData : GameData(
    id = "brawl",
    name = "Brawl",
    votable = true,
    maps = setOf(Map("district")),
    cutsceneSteps = arrayListOf(
        CutsceneStep(
            Component.empty()
                .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
                .append(Format.mm("<glyph:game/brawl_icon>"))
                .append(Component.text(" Brawl")),
            "cutscene.start"
        ) { _ ->
            delay(5000)
        },
        CutsceneStep(Format.mm(
            "In this game, you choose a <yellow>kit</yellow> to bring into battle<br>" +
                "The kits you can choose from are <aqua>chosen randomly</aqua> for each round, each providing a <green>unique playstyle</green>"
        ), "cutscene.start") {
            delay(5000)
        },
        CutsceneStep(Format.mm(
            "After <yellow>1 minute</yellow>, a <red>border</red> will start shrinking in from the edges of the map.<br>" +
                    "Whichever team outlasts all the other ones will be the <yellow>victor!</yellow>"
        ), "cutscene.start") {
            delay(5000)
        },
        CutsceneStep.GLHF,
    ),
    flags = setOf(
        Flag.ENABLE_HUNGER,
        Flag.ENABLE_ITEM_DROPS
    ),
    scores = hashMapOf(
        CommonScoreSource.KILL to 180,
        CommonScoreSource.OUTLAST to 10,
        BrawlController.BrawlScoreSource.SURVIVE_ONE_MINUTE to 120,
    ),
    spawns = BrawlSpawn.entries,
    scoreboard = BrawlScoreboard::class,
)