package xyz.devcmb.font

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

object FontOverrides {
    fun getOverrides(file: File, defaultHeight: Int, defaultAscent: Int): Pair<Int, Int> {
        var config = File(file.parent, "${file.name}.overrides.json")
        if(!config.exists()) {
            val parentOverrides = File(file.parent, "folder_overrides.json")
            if(parentOverrides.exists()) {
                config = parentOverrides
            } else return defaultHeight to defaultAscent
        }

        val data = Json.decodeFromString<FontOverrideData>(config.readText())
        return (data.height ?: defaultHeight) to (data.ascent ?: defaultAscent)
    }

    @Serializable
    data class FontOverrideData(
        val height: Int? = null,
        val ascent: Int? = null
    )
}
