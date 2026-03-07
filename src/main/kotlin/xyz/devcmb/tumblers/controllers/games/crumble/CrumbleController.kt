package xyz.devcmb.tumblers.controllers.games.crumble

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    votable = true,
    flags = setOf(Flag.HUNGER_REMOVED),
    maps = setOf(Map("warfare")),
    rounds = 7,
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.text("Welcome to Crumble", NamedTextColor.YELLOW)) { map ->
            teleport(0.0,0.0,0.0,0f,0f)
            delay(5000)
        },
        CutsceneStep(Component.text("Cutscene step #2", NamedTextColor.GRAY)) { map ->
            teleport(0.0,128.0,0.0,0f,0f)
            delay(2000)
        }
    )
) {
    override suspend fun spawn() {
        // TODO
    }
}