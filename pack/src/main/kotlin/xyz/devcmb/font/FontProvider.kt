package xyz.devcmb.font

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.devcmb.util.IdentifiedResource

@Serializable
sealed interface FontProvider {
    /**
     * [file] is relative to assets/namespace/textures and should contain a file extension
     */
    @Serializable
    @SerialName("bitmap")
    class BitmapFontProvider(
        val file: IdentifiedResource,
        val height: Int,
        val ascent: Int,
        val chars: List<String>
    ) : FontProvider

    @Serializable
    @SerialName("reference")
    class ReferenceFontProvider(
        val id: IdentifiedResource
    ) : FontProvider

    @Serializable
    @SerialName("space")
    class SpaceFontProvider(
        val advances: Map<String, Int>
    ): FontProvider
}