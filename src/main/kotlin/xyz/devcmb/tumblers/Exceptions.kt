package xyz.devcmb.tumblers

import kotlin.Exception

class GameOperatorException(override val message: String) : Exception()
class GameControllerException(override val message: String) : Exception()
class WorldCreationException(override val message: String) : Exception()
class MapSetupException(override val message: String) : Exception()