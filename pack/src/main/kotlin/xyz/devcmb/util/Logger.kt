package xyz.devcmb.util

object Logger {
    fun info(text: String) {
        println("\u001B[36m[INFO]\u001B[0m $text")
    }

    fun warn(text: String) {
        println("\u001B[33m[WARN]\u001B[0m $text")
    }

    fun error(text: String) {
        println("\u001B[31m[ERROR]\u001B[0m $text")
    }

    fun success(text: String) {
        println("\u001B[32m[SUCCESS]\u001B[0m $text")
    }
}