package org.multipaz.provisioning.openid4vci

import org.multipaz.util.toBase64Url
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProofJwtHeaderParserTest {
    @Test
    fun extractKidFromBase64HeaderObject() {
        val header = """{"alg":"ES256","kid":"test-kid"}"""
            .encodeToByteArray()
            .toBase64Url()
        val jwt = "$header.payload.signature"
        assertEquals("test-kid", ProofJwtHeaderParser.extractKid(jwt))
    }

    @Test
    fun extractKidFromRawHeaderObjectFallback() {
        val jwt = """{"jwk":{"kid":"inner-kid"}}.payload.signature"""
        assertEquals("inner-kid", ProofJwtHeaderParser.extractKid(jwt))
    }

    @Test
    fun returnsNullForJsonLiteralHeader() {
        val jwt = "\"not-an-object\".payload.signature"
        assertNull(ProofJwtHeaderParser.extractKid(jwt))
    }

    @Test
    fun returnsNullForMalformedHeader() {
        val jwt = "!!!.payload.signature"
        assertNull(ProofJwtHeaderParser.extractKid(jwt))
    }
}
