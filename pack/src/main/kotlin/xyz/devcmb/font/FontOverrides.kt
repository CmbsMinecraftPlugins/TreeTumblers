package xyz.devcmb.font

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import xyz.devcmb.util.Logger
import java.io.File

object FontOverrides {
    fun getOverrides(file: File, defaultHeight: Int, defaultAscent: Int): List<Pair<Int, Int>> {
        require(defaultHeight >= defaultAscent) {
            Logger.error("Provided default height $defaultHeight was not greater than or equal to provided default ascent $defaultAscent")
            "Default ascent must not be higher than default height"
        }

        var config = File(file.parent, "${file.name}.overrides.json")
        if(!config.exists()) {
            val parentOverrides = File(file.parent, "folder_overrides.json")
            if(parentOverrides.exists()) {
                config = parentOverrides
            } else return listOf(defaultHeight to defaultAscent)
        }

        val element: JsonElement = Json.parseToJsonElement(config.readText())
        val overrides: List<FontOverrideData> = when(element) {
            is JsonArray -> Json.decodeFromJsonElement(element)
            is JsonObject -> listOf(Json.decodeFromJsonElement(element))
            else -> emptyList()
        }


        return overrides.map { (it.height ?: defaultHeight) to (it.ascent ?: defaultAscent) }
    }

    @Serializable
    data class FontOverrideData(
        val height: Int? = null,
        val ascent: Int? = null
    )
}
