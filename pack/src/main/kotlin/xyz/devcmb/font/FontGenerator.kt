package xyz.devcmb.font

import xyz.devcmb.pack.ResourcePackBuilder

interface FontGenerator {
    fun generateFonts(builder: ResourcePackBuilder): Iterable<GeneratedFont>
}