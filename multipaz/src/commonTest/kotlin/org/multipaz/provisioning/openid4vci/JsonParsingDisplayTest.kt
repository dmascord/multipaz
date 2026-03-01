package org.multipaz.provisioning.openid4vci

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.multipaz.crypto.Algorithm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class JsonParsingDisplayTest {
    private val clientPreferences = OpenID4VCIClientPreferences(
        clientId = "urn:test:client",
        redirectUrl = "test://callback",
        locales = listOf("en-US"),
        signingAlgorithms = listOf(Algorithm.ESP256)
    )

    @Test
    fun extractDisplayAcceptsPercentEncodedBase64DataUri() = runBlocking {
        val element = buildJsonObject {
            put(
                "display",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("locale", JsonPrimitive("en-US"))
                            put("name", JsonPrimitive("Photo ID"))
                            put(
                                "logo",
                                buildJsonObject {
                                    put("uri", JsonPrimitive("data:text/plain;base64,SGVsbG8%3D"))
                                }
                            )
                        }
                    )
                )
            )
        }
        val display = JsonParsing("test").extractDisplay(element, clientPreferences)
        assertEquals("Photo ID", display.text)
        val logo = assertNotNull(display.logo)
        assertEquals("Hello", logo.toByteArray().decodeToString())
    }

    @Test
    fun extractDisplayDecodesPercentEscapesInPlainDataUri() = runBlocking {
        val element = buildJsonObject {
            put(
                "display",
                JsonArray(
                    listOf(
                        buildJsonObject {
                            put("locale", JsonPrimitive("en-US"))
                            put("name", JsonPrimitive("Photo ID"))
                            put(
                                "logo",
                                buildJsonObject {
                                    put("uri", JsonPrimitive("data:text/plain,Hello%20World"))
                                }
                            )
                        }
                    )
                )
            )
        }
        val display = JsonParsing("test").extractDisplay(element, clientPreferences)
        assertEquals("Photo ID", display.text)
        val logo = assertNotNull(display.logo)
        assertEquals("Hello World", logo.toByteArray().decodeToString())
    }
}
