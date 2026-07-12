package xyz.devcmb.util

import xyz.devcmb.font.FontOverrides
import xyz.devcmb.font.FontProvider
import xyz.devcmb.pack.ResourcePackBuilder
import java.io.File

fun Int.toUnicode(): String {
    return String(Character.toChars(this))
}