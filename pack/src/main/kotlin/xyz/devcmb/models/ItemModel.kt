package xyz.devcmb.models

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.devcmb.util.IdentifiedResource

class ItemModel(val textureLocation: IdentifiedResource) {
    fun getSerialized(): String {
        return Json.encodeToString(ItemModelData(
            mapOf("layer0" to
                    textureLocation.namespace.name.lowercase() + ":" + textureLocation.resourcePath.parts.drop(1).joinToString("/")
            ),
        ))
    }

    @Serializable
    class ItemModelData(
        val textures: Map<String, String>,
        @EncodeDefault val parent: String = "minecraft:item/generated",
        @EncodeDefault val overrides: List<String> = emptyList(),
    )
}