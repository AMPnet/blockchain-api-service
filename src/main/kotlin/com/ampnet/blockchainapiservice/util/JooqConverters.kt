package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.config.JsonConfig
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.fasterxml.jackson.databind.JsonNode
import org.jooq.Converter
import org.jooq.JSON
import java.math.BigInteger
import java.time.OffsetDateTime

class ChainIdConverter : Converter<Long, ChainId> {
    override fun from(value: Long?): ChainId? = value?.let { ChainId(it) }
    override fun to(value: ChainId?): Long? = value?.value
    override fun fromType(): Class<Long> = Long::class.java
    override fun toType(): Class<ChainId> = ChainId::class.java

    companion object {
        private const val serialVersionUID: Long = -7367190641407949448L
    }
}

class ContractAddressConverter : Converter<String, ContractAddress> {
    override fun from(value: String?): ContractAddress? = value?.let { ContractAddress(it) }
    override fun to(value: ContractAddress?): String? = value?.rawValue
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<ContractAddress> = ContractAddress::class.java

    companion object {
        private const val serialVersionUID: Long = 5587113562689277144L
    }
}

class WalletAddressConverter : Converter<String, WalletAddress> {
    override fun from(value: String?): WalletAddress? = value?.let { WalletAddress(it) }
    override fun to(value: WalletAddress?): String? = value?.rawValue
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<WalletAddress> = WalletAddress::class.java

    companion object {
        private const val serialVersionUID: Long = 4048003464768439754L
    }
}

class BalanceConverter : Converter<BigInteger, Balance> {
    override fun from(value: BigInteger?): Balance? = value?.let { Balance(it) }
    override fun to(value: Balance?): BigInteger? = value?.rawValue
    override fun fromType(): Class<BigInteger> = BigInteger::class.java
    override fun toType(): Class<Balance> = Balance::class.java

    companion object {
        private const val serialVersionUID: Long = 6133349110900653628L
    }
}

class BlockNumberConverter : Converter<BigInteger, BlockNumber> {
    override fun from(value: BigInteger?): BlockNumber? = value?.let { BlockNumber(it) }
    override fun to(value: BlockNumber?): BigInteger? = value?.value
    override fun fromType(): Class<BigInteger> = BigInteger::class.java
    override fun toType(): Class<BlockNumber> = BlockNumber::class.java

    companion object {
        private const val serialVersionUID: Long = 154324798075521810L
    }
}

class TransactionHashConverter : Converter<String, TransactionHash> {
    override fun from(value: String?): TransactionHash? = value?.let { TransactionHash(it) }
    override fun to(value: TransactionHash?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<TransactionHash> = TransactionHash::class.java

    companion object {
        private const val serialVersionUID: Long = -4101282162822045477L
    }
}

class SignedMessageConverter : Converter<String, SignedMessage> {
    override fun from(value: String?): SignedMessage? = value?.let { SignedMessage(it) }
    override fun to(value: SignedMessage?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<SignedMessage> = SignedMessage::class.java

    companion object {
        private const val serialVersionUID: Long = 8287866421855372066L
    }
}

class DurationSecondsConverter : Converter<BigInteger, DurationSeconds> {
    override fun from(value: BigInteger?): DurationSeconds? = value?.let { DurationSeconds(it) }
    override fun to(value: DurationSeconds?): BigInteger? = value?.rawValue
    override fun fromType(): Class<BigInteger> = BigInteger::class.java
    override fun toType(): Class<DurationSeconds> = DurationSeconds::class.java

    companion object {
        private const val serialVersionUID: Long = 442010490221114533L
    }
}

class JsonNodeConverter : Converter<JSON, JsonNode> {

    private val objectMapper = JsonConfig().objectMapper()

    override fun from(value: JSON?): JsonNode? = value?.let { objectMapper.readTree(it.data()) }
    override fun to(value: JsonNode?): JSON? = value?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) }
    override fun fromType(): Class<JSON> = JSON::class.java
    override fun toType(): Class<JsonNode> = JsonNode::class.java

    companion object {
        private const val serialVersionUID: Long = 912286490023352427L
    }
}

class UtcDateTimeConverter : Converter<OffsetDateTime, UtcDateTime> {
    override fun from(value: OffsetDateTime?): UtcDateTime? = value?.let { UtcDateTime(it) }
    override fun to(value: UtcDateTime?): OffsetDateTime? = value?.value
    override fun fromType(): Class<OffsetDateTime> = OffsetDateTime::class.java
    override fun toType(): Class<UtcDateTime> = UtcDateTime::class.java

    companion object {
        private const val serialVersionUID: Long = -8529019904691898554L
    }
}

class BaseUrlConverter : Converter<String, BaseUrl> {
    override fun from(value: String?): BaseUrl? = value?.let { BaseUrl(it) }
    override fun to(value: BaseUrl?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<BaseUrl> = BaseUrl::class.java

    companion object {
        private const val serialVersionUID: Long = -593346707327357156L
    }
}

class ContractIdConverter : Converter<String, ContractId> {
    override fun from(value: String?): ContractId? = value?.let { ContractId(it) }
    override fun to(value: ContractId?): String? = value?.value
    override fun fromType(): Class<String> = String::class.java
    override fun toType(): Class<ContractId> = ContractId::class.java

    companion object {
        private const val serialVersionUID: Long = -3208788163001411967L
    }
}

class ContractBinaryDataConverter : Converter<ByteArray, ContractBinaryData> {
    override fun from(value: ByteArray?): ContractBinaryData? = value?.let { ContractBinaryData(it) }
    override fun to(value: ContractBinaryData?): ByteArray? = value?.binary
    override fun fromType(): Class<ByteArray> = ByteArray::class.java
    override fun toType(): Class<ContractBinaryData> = ContractBinaryData::class.java

    companion object {
        private const val serialVersionUID: Long = -1908613704199073695L
    }
}

class FunctionDataConverter : Converter<ByteArray, FunctionData> {
    override fun from(value: ByteArray?): FunctionData? = value?.let { FunctionData(it) }
    override fun to(value: FunctionData?): ByteArray? = value?.binary
    override fun fromType(): Class<ByteArray> = ByteArray::class.java
    override fun toType(): Class<FunctionData> = FunctionData::class.java

    companion object {
        private const val serialVersionUID: Long = -5671993772596790084L
    }
}

class ManifestJsonConverter : Converter<JSON, ManifestJson> {

    private val objectMapper = JsonConfig().objectMapper()

    override fun from(value: JSON?): ManifestJson? = value?.let {
        objectMapper.readValue(it.data(), ManifestJson::class.java)
    }

    override fun to(value: ManifestJson?): JSON? = value?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) }
    override fun fromType(): Class<JSON> = JSON::class.java
    override fun toType(): Class<ManifestJson> = ManifestJson::class.java

    companion object {
        private const val serialVersionUID: Long = -1324416320471064842L
    }
}

class ArtifactJsonConverter : Converter<JSON, ArtifactJson> {

    private val objectMapper = JsonConfig().objectMapper()

    override fun from(value: JSON?): ArtifactJson? = value?.let {
        objectMapper.readValue(it.data(), ArtifactJson::class.java)
    }

    override fun to(value: ArtifactJson?): JSON? = value?.let { JSON.valueOf(objectMapper.writeValueAsString(it)) }
    override fun fromType(): Class<JSON> = JSON::class.java
    override fun toType(): Class<ArtifactJson> = ArtifactJson::class.java

    companion object {
        private const val serialVersionUID: Long = 2789587672933464251L
    }
}
