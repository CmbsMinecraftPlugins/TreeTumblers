package xyz.devcmb.tumblers.controllers.games.crumble

import kotlinx.coroutines.delay
import net.kyori.adventure.text.Component
import net.kyori.adventure.text.format.NamedTextColor
import org.bukkit.entity.Player
import xyz.devcmb.tumblers.GameControllerException
import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.data.Team
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.map.Map
import xyz.devcmb.tumblers.engine.cutscene.CutsceneStep
import xyz.devcmb.tumblers.engine.map.spawn.SpawnGroup

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    votable = true,
    flags = setOf(Flag.HUNGER_REMOVED),
    maps = setOf(
        Map(
            "warfare",
            listOf(
                SpawnGroup(
                    "spawns.pregame",
                    SpawnGroup.SpawnType.FIXED
                )
            ),
            listOf(
                SpawnGroup(
                    "spawns.ingame.arena1",
                    SpawnGroup.SpawnType.MATCHUP
                )
            )
        )
    ),
    rounds = 7,
    cutsceneSteps = arrayListOf(
        CutsceneStep(Component.text("Welcome to Crumble", NamedTextColor.YELLOW)) { map ->
            teleportConfig("cutscene.start")
            delay(5000)
        },
        CutsceneStep(Component.text("Cutscene step #2", NamedTextColor.GRAY)) { map ->
            teleport(0.0,128.0,0.0,0f,0f)
            delay(2000)
        }
    )
) {
    override suspend fun pregame() {
        super.pregame()
    }
}