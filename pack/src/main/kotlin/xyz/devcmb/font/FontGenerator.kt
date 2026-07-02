package xyz.devcmb.font

interface FontGenerator {
    fun generateFonts(): Iterable<GeneratedFont>
}