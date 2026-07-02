package xyz.devcmb.items

import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import xyz.devcmb.util.IdentifiedResource

class ItemModelReference(val modelLocation: IdentifiedResource) {
    fun getSerialized(): String {
        return Json.encodeToString(ItemModelReferenceData(
            ItemModelReferenceData.ModelData(
                modelLocation.namespace.name.lowercase() + ":" + modelLocation.resourcePath.parts.drop(1).joinToString("/")
            )
        ))
    }

    @Serializable
    class ItemModelReferenceData(
        val model: ModelData,
        @EncodeDefault val oversized_in_gui: Boolean = true
    ) {
        @Serializable
        data class ModelData(
            val model: String,
            @EncodeDefault val type: String = "model",
        )
    }
}