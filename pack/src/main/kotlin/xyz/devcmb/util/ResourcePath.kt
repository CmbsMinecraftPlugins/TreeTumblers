package xyz.devcmb.util

class ResourcePath(vararg parts: String) {
    constructor(path: String): this(*path.split("/").toTypedArray())

    val path = parts.joinToString("/")
}