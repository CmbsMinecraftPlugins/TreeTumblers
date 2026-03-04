package xyz.devcmb.tumblers

import kotlin.Exception

class GameOperatorException(override val message: String) : Exception()
class GameControllerException(override val message: String) : Exception()
class WorldCreationError(override val message: String) : Exception()