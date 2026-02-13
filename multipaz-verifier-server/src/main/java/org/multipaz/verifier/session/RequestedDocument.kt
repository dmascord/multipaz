package org.multipaz.verifier.session

import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.openid.TransactionData

@CborSerializable
data class RequestedDocument(
    val id: String,
    val format: String,  // "dc+sd-jwt" or "mso_mdoc"
    val multiple: Boolean,
    val claims: List<RequestedClaim>
) {
    companion object
}