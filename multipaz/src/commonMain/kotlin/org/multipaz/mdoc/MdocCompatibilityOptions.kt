package org.multipaz.mdoc

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
)
