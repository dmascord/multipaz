package org.multipaz.mdoc

enum class OpenId4VpDraft18TranscriptMode {
    CREDO,
    GENERATED_NONCE_THIRD_ENTRY,
}

/**
 * Compatibility toggles for ISO 18013-5 parsing/verification.
 */
data class MdocCompatibilityOptions(
    /**
     * Allows parsing `ValidityInfo` timestamps that omit the required tdate tag or contain
     * fractional seconds. Disabled by default to match ISO 18013-5 clause 9.1.2.4.
     *
     * This is a temporary workaround for the NEC/IATA HKG mdoc PhotoID issuer on
     * https://mdoc.sitalab.io which still emits untagged timestamps. The issuer committed to
     * deploying a fix before 2026-07-01 (see issue #1541). Remove once they confirm compliance.
     */
    val allowLegacyMsoValidityTimestamps: Boolean = false,
    /**
     * Allows decoding IssuerAuth payloads that omit the required `#6.24(bstr .cbor MSO)` wrapper
     * defined in ISO/IEC 18013-5 clause 9.1.2.4 and instead embed the MSO CBOR directly.
     * Disabled by default so we fail fast when issuers violate the spec.
     */
    val allowLegacyMsoPayloadWithoutTag24: Boolean = false,
    /**
     * Controls the legacy OpenID4VP Draft 18 / ISO 18013-7 redirect session-transcript layout.
     *
     * `CREDO` matches the public Credo/Paradym model:
     * `[sha256(cbor([clientId, mdocGeneratedNonce])), sha256(cbor([responseUri, mdocGeneratedNonce])), verifierNonce]`
     *
     * `GENERATED_NONCE_THIRD_ENTRY` keeps the hashed pair inputs the same, but places the
     * wallet-generated nonce in the third array slot. This is an experimental interoperability
     * fallback for verifiers that appear to compare JWE `apu` against the transcript's third value.
     */
    val openId4VpDraft18TranscriptMode: OpenId4VpDraft18TranscriptMode = OpenId4VpDraft18TranscriptMode.CREDO,
)
