package xyz.devcmb.util

class ResourcePath(vararg val parts: String) {
    constructor(path: String): this(*path.split("/").toTypedArray())

    val path = parts.joinToString("/")
}