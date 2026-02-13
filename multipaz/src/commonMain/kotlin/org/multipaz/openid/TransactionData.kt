package org.multipaz.openid

import kotlinx.io.bytestring.ByteString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.multipaz.cbor.annotation.CborSerializable
import org.multipaz.crypto.Algorithm
import org.multipaz.crypto.Crypto
import org.multipaz.util.fromBase64Url

class TransactionData(
    val hash: ByteString,
    val type: String,
    val data: JsonObject
) {
    companion object {
        suspend fun parse(transactionData: JsonElement): Map<String, List<TransactionData>> {
            transactionData as? JsonArray
                ?: throw IllegalArgumentException("Invalid transaction_data")
            return parse(transactionData.map { it.jsonPrimitive.content })
        }

        suspend fun parse(transactionData: List<String>): Map<String, List<TransactionData>> {
            val map = mutableMapOf<String, MutableList<TransactionData>>()
            for(encoded in transactionData) {
                val jsonText = encoded.fromBase64Url().decodeToString()
                val data = Json.parseToJsonElement(jsonText).jsonObject
                val type = data["type"]!!.jsonPrimitive.content
                if (type != "multipaz_test") {
                    throw IllegalArgumentException("Unsupported transaction type: '$type'")
                }
                // TODO: support transaction_data_hashes_alg
                val hash = Crypto.digest(Algorithm.SHA256, encoded.encodeToByteArray())
                val parsed = TransactionData(ByteString(hash), type, data)
                for (id in data["credential_ids"]!!.jsonArray) {
                    map.getOrPut(id.jsonPrimitive.content) { mutableListOf() }.add(parsed)
                }
            }
            return map.mapValues { (_, list) -> list.toList() }
        }
    }
}