package org.multipaz.mdoc.mso

import org.multipaz.cbor.Bstr
import org.multipaz.cbor.Cbor
import org.multipaz.cbor.DataItem
import org.multipaz.cbor.Tagged
import org.multipaz.mdoc.MdocCompatibilityOptions
import org.multipaz.util.Logger

internal object MsoPayloadDecoder {
    private const val TAG = "MsoPayloadDecoder"

    /**
     * Accepts both spec-compliant payloads (#6.24(bstr .cbor MSO)) and legacy/raw CBOR MSO payloads.
     */
    fun decode(
        payload: ByteArray,
        compatibilityOptions: MdocCompatibilityOptions = MdocCompatibilityOptions()
    ): DataItem {
        val decoded = Cbor.decode(payload)
        return decodeDataItem(decoded, compatibilityOptions)
    }

    private tailrec fun decodeDataItem(
        item: DataItem,
        compatibilityOptions: MdocCompatibilityOptions,
        depth: Int = 0
    ): DataItem {
        return when (item) {
            is Tagged -> {
                val taggedItem = item.taggedItem
                if (item.tagNumber == Tagged.ENCODED_CBOR && taggedItem is Bstr) {
                    decodeDataItem(Cbor.decode(taggedItem.asBstr), compatibilityOptions, depth + 1)
                } else {
                    throw IllegalArgumentException(
                        "IssuerAuth payload must be tag 24 encoded CBOR MSO (tag ${item.tagNumber})"
                    )
                }
            }
            is Bstr -> {
                if (!compatibilityOptions.allowLegacyMsoPayloadWithoutTag24) {
                    throw IllegalArgumentException("IssuerAuth payload must be tag 24 encoded CBOR MSO")
                }
                if (depth == 0) {
                    Logger.w(
                        TAG,
                        "Allowing legacy MSO payload without tag 24; remove after 2026-07-01"
                    )
                }
                decodeDataItem(Cbor.decode(item.asBstr), compatibilityOptions, depth + 1)
            }
            else -> item
        }
    }

}
