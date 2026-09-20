package com.wapo.kmpshared.util.serializer

import com.wapo.kmpshared.util.KMPURL
import com.wapo.kmpshared.util.createKMPURLfromString
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object KMPURLSerializer : KSerializer<KMPURL> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("KMPURL", PrimitiveKind.STRING)

    override fun serialize(
        encoder: Encoder,
        value: KMPURL,
    ) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): KMPURL {
        val stringValue = decoder.decodeString()
        return createKMPURLfromString(stringValue)
            ?: throw IllegalArgumentException("Malformed URL: $stringValue")
    }
}
