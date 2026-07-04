package xyz.devcmb.tumblers.controllers.games.brawl

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
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
    ),
    flags = setOf(
        Flag.USE_SPECTATOR_DEATH_SYSTEM_NO_ACTIONBAR,
        Flag.ENABLE_HUNGER,
        Flag.HIDE_ENEMY_NAMETAGS,
        Flag.ENABLE_ITEM_DROPS
    ),
    scores = hashMapOf(),
    icon = Format.mm("<glyph:game/brawl_icon>"),
    logo = Format.mm("<glyph:game/brawl_logo>"),
    tabLogo = Format.mm("<glyph:game/brawl_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    spawns = BrawlSpawn.entries,
    scoreboard = "brawlScoreboard",
)