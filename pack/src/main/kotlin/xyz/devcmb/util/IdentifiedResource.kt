package xyz.devcmb.util

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind.STRING
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

@Serializable(with = IdentifiedResourceSerializer::class)
data class IdentifiedResource(
    val path: String
) {
    @Transient
    val namespace: Namespace = Namespace.entries.find { it.name.equals(path.substringBefore(':'), true) }!!

    @Transient
    val resourcePath: ResourcePath = ResourcePath(path.substringAfter(":"))

    constructor(namespace: Namespace, resourcePath: ResourcePath)
            : this("${namespace.name.lowercase()}:${resourcePath.path}")
}

object IdentifiedResourceSerializer : KSerializer<IdentifiedResource> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IdentifiedResource", STRING)

    override fun serialize(
        encoder: Encoder,
        value: IdentifiedResource
    ) {
        encoder.encodeString(value.path)
    }

    override fun deserialize(
        decoder: Decoder
    ): IdentifiedResource {
        return IdentifiedResource(decoder.decodeString())
    }
}