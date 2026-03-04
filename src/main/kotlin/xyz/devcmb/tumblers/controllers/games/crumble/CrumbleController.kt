package xyz.devcmb.tumblers.controllers.games.crumble

import xyz.devcmb.tumblers.annotations.EventGame
import xyz.devcmb.tumblers.engine.Flag
import xyz.devcmb.tumblers.engine.GameBase
import xyz.devcmb.tumblers.engine.Map

@EventGame
class CrumbleController : GameBase(
    id = "crumble",
    votable = true,
    flags = setOf(Flag.HUNGER_REMOVED),
    maps = setOf(Map("crumble_warfare", "maps.crumble.warfare"))
)