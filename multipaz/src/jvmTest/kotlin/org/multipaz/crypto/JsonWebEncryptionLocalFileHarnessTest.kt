package org.multipaz.crypto

import kotlinx.coroutines.test.runTest
import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import org.multipaz.testUtilSetupCryptoProvider
import org.multipaz.util.fromBase64Url
import kotlin.io.path.Path
import kotlin.io.path.readText
import kotlin.io.path.writeText
import kotlin.test.BeforeTest
import kotlin.test.Test

class JsonWebEncryptionLocalFileHarnessTest {
    @BeforeTest
    fun setup() = testUtilSetupCryptoProvider()

    @Test
    fun encrypt_fromRequestFile_writesResponseFile() = runTest {
        val requestPath = System.getProperty("jwe.request")
            ?: System.getenv("JWE_REQUEST")
            ?: error("Missing jwe.request / JWE_REQUEST")
        val responsePath = System.getProperty("jwe.response")
            ?: System.getenv("JWE_RESPONSE")
            ?: error("Missing jwe.response / JWE_RESPONSE")
        val request = Json.decodeFromString(JsonObject.serializer(), Path(requestPath).readText())

        val encryptedJwt = JsonWebEncryption.encrypt(
            claimsSet = request["claimsSet"]!!.jsonObject,
            recipientPublicKey = EcPublicKey.fromJwk(request["recipientPublicJwk"]!!.jsonObject),
            encAlg = Algorithm.valueOf(request["encAlg"]!!.jsonPrimitive.content),
            apu = request["apu"]?.jsonPrimitive?.content?.let { ByteString(it.fromBase64Url()) },
            apv = request["apv"]?.jsonPrimitive?.content?.let { ByteString(it.fromBase64Url()) },
            kid = request["kid"]?.jsonPrimitive?.content,
            compressionLevel = request["compressionLevel"]?.jsonPrimitive?.content?.toInt()
        )

        val protectedHeader = encryptedJwt.split('.')
            .first()
            .fromBase64Url()
            .decodeToString()

        val output = buildJsonObject {
            put("encryptedJwt", JsonPrimitive(encryptedJwt))
            put("protectedHeader", Json.decodeFromString(JsonObject.serializer(), protectedHeader))
        }
        Path(responsePath).writeText(Json.encodeToString(output))
        println(Json.encodeToString(output))
    }
}
