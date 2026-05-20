package xyz.devcmb.tumblers

open class TumblingException(message: String) : RuntimeException(message)
open class TumblingDatabaseException(message: String) : TumblingException(message)
open class TumblingGameException(message: String) : TumblingException(message)
open class TumblingWorldException(message: String) : TumblingException(message)
open class TumblingEventException(message: String) : TumblingException(message)
open class TumblingUIException(message: String) : TumblingException(message)

class GameOperatorException(message: String) : TumblingGameException(message)
class GameControllerException(message: String) : TumblingGameException(message)

class WorldCreationException(message: String) : TumblingWorldException(message)
class MapSetupException(message: String) : TumblingWorldException(message)

class TumblingDatabaseStateException(message: String) : TumblingDatabaseException(message)


class TumblingConfigKeyMissingException(key: String)
    : TumblingException("Configuration did not have a valid value for key $key")
class TumblingConfigTypeMismatchException(key: String, expected: String, actual: String)
    : TumblingException("Expected config key $key to return type $expected, got $actual")