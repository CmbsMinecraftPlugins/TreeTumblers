package xyz.devcmb.font

import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient
import xyz.devcmb.util.IdentifiedResource

@Serializable
data class GeneratedFont(
    @Transient
    val resource: IdentifiedResource = IdentifiedResource(""),
    val providers: List<FontProvider>
)