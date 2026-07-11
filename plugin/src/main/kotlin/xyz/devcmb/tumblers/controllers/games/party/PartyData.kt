package xyz.devcmb.tumblers.controllers.games.party

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import net.kyori.adventure.text.format.ShadowColor
import xyz.devcmb.tumblers.controllers.games.party.PartyController.PartyScoreSource
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameData
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.util.Format

object PartyData : GameData(
    id = "party",
    name = "Party",
    votable = true,
    maps = setOf(
        Map("main")
    ),
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.empty()
            .append(Component.text("Welcome to ", NamedTextColor.YELLOW))
            .append(Format.mm("<glyph:game/party_icon>"))
            .append(Component.text(" Party")),
            "cutscene.start"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("In this game, <yellow>you</yellow> and <yellow>your team</yellow> will fight in head-to-head <aqua>minigames!</aqua>"),
            "cutscene.first"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("This game comes in <aqua>2 parts...</aqua>"),
            "cutscene.second"
        ) {
            delay(2500)
        },
        CutsceneStep(
            Format.mm("You start playing <yellow>individual games</yellow> where you fight one other person.<br>This stage lasts the first <aqua>5m</aqua> of the game."),
            "cutscene.third"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("Then, you will transition to playing <yellow>team games</yellow> where you fight against a whole team.<br>This stage lasts the final <aqua>5m</aqua> of the game."),
            "cutscene.fourth"
        ) {
            delay(5000)
        },
        CutsceneStep(
            Format.mm("Game range from <aqua>Sword duels</aqua> to <aqua>Mace duels</aqua> and anything in between!<br>While you're waiting for a match, you'll be waiting here."),
            "cutscene.start"
        ) {
            delay(5000)
        },
        CutsceneStep.GLHF
    ),
    scores = hashMapOf(
        PartyScoreSource.INDIVIDUAL_GAME_WIN to 80,
        PartyScoreSource.INDIVIDUAL_GAME_DRAW to 40,
        PartyScoreSource.TEAM_GAME_WIN to 240,
        PartyScoreSource.TEAM_GAME_DRAW to 160
    ),
    flags = setOf(
        Flag.DISABLE_FALL_DAMAGE,
        Flag.DISABLE_BLOCK_BREAKING,
        Flag.DISABLE_NATURAL_REGENERATION
    ),
    icon = Format.mm("<glyph:game/party_icon>"),
    logo = Format.mm("<glyph:game/party_logo>"),
    tabLogo = Format.mm("<glyph:game/party_logo_14a_45h>")
        .shadowColor(ShadowColor.none()),
    scoreboard = PartyScoreboard::class
)