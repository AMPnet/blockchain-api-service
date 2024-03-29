package com.ampnet.blockchainapiservice.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration
import java.math.BigInteger
import java.nio.file.Path
import java.time.Duration

@Configuration
@ConfigurationProperties(prefix = "blockchain-api-service")
class ApplicationProperties {
    val jwt = JwtProperties()
    val chainEthereum = ChainProperties()
    val chainGoerli = ChainProperties()
    val chainMatic = ChainProperties()
    val chainMumbai = ChainProperties()
    val chainHardhatTestnet = ChainProperties()
    val chainBsc = ChainProperties()
    val chainXdai = ChainProperties()
    val chainFantom = ChainProperties()
    val chainMoonriver = ChainProperties()
    val chainAvalanche = ChainProperties()
    val chainAurora = ChainProperties()
    var infuraId: String = ""
    val contractDecorators = ContractDecoratorProperties()
    val metaPixelProperties = MetaPixelProperties()
    val contractManifestService = ContractManifestServiceProperties()
}

class JwtProperties {
    lateinit var publicKey: String
}

@Suppress("MagicNumber")
class ChainProperties {
    var minBlockConfirmationsForCaching: BigInteger? = null
    var latestBlockCacheDuration: Duration = Duration.ofSeconds(5L)
    var rpcUrlOverride: String? = null
    var startBlockNumber: BigInteger? = null
}

@Suppress("MagicNumber")
class ContractDecoratorProperties {
    var rootDirectory: Path? = null
    var ignoredDirs: List<String> = listOf(".git")
    var fillChangePollInterval: Duration = Duration.ofMinutes(1L)
    var fileChangeQuietInterval: Duration = Duration.ofSeconds(30L)
}

class MetaPixelProperties {
    var accessToken: String? = null
    var pixelId: String? = null
}

class ContractManifestServiceProperties {
    var baseUrl: String? = null
    var decompileContractPath = "/decompile-contract"
}
