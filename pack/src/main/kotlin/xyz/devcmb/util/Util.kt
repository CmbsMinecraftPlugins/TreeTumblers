package xyz.devcmb.util

import xyz.devcmb.font.FontOverrides
import xyz.devcmb.font.FontProvider
import xyz.devcmb.pack.ResourcePackBuilder
import java.io.File

fun Int.toUnicode(): String {
    return String(Character.toChars(this))
}

fun dirToBitmapProviders(
    builder: ResourcePackBuilder,
    root: File,
    defaultHeight: Int = 9,
    defaultAscent: Int = 8,
    addTextures: Boolean = true
): ArrayList<FontProvider.BitmapFontProvider> {
    var currentIndex = 0
    val providers = arrayListOf<FontProvider.BitmapFontProvider>()
    fun scanDir(file: File) {
        file.listFiles().filter { it.name.endsWith(".png") || it.isDirectory }.forEach {
            if(it.isDirectory) {
                scanDir(it)
                return@forEach
            }

            val name = it.path.toString()
                .substringAfter(root.path.toString() + File.separator)
                .replace(File.separator, "_")
            val resource = IdentifiedResource(Namespace.TUMBLING, ResourcePath("font", root.name, name))
            if(addTextures) builder.addTexture(it, resource)

            val overrides: List<Pair<Int, Int>> = FontOverrides.getOverrides(it, defaultHeight, defaultAscent)
            overrides.forEach { (height, ascent) ->
                providers.add(FontProvider.BitmapFontProvider(
                    resource,
                    height, ascent,
                    listOf((currentIndex + 0xF000).toUnicode())
                ))
                currentIndex++
            }
        }
    }
    scanDir(root)
    return providers
}