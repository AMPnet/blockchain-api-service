package com.ampnet.blockchainapiservice.blockchain

import com.ampnet.blockchainapiservice.blockchain.properties.ChainPropertiesHandler
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.config.ApplicationProperties
import com.ampnet.blockchainapiservice.exception.BlockchainReadException
import com.ampnet.blockchainapiservice.exception.TemporaryBlockchainReadException
import com.ampnet.blockchainapiservice.model.params.ExecuteReadonlyFunctionCallParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentTransactionInfo
import com.ampnet.blockchainapiservice.model.result.ReadonlyFunctionCallResult
import com.ampnet.blockchainapiservice.repository.Web3jBlockchainServiceCacheRepository
import com.ampnet.blockchainapiservice.service.AbiDecoderService
import com.ampnet.blockchainapiservice.service.UtcDateTimeProvider
import com.ampnet.blockchainapiservice.service.UuidProvider
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BinarySearch
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.BlockParameter
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
import com.ampnet.blockchainapiservice.util.bind
import com.ampnet.blockchainapiservice.util.shortCircuiting
import mu.KLogging
import org.springframework.stereotype.Service
import org.web3j.protocol.Web3j
import org.web3j.protocol.core.DefaultBlockParameter
import org.web3j.protocol.core.RemoteFunctionCall
import org.web3j.protocol.core.Request
import org.web3j.protocol.core.Response
import org.web3j.protocol.core.methods.request.Transaction
import org.web3j.protocol.core.methods.response.EthBlock
import org.web3j.tx.ReadonlyTransactionManager
import org.web3j.tx.gas.DefaultGasProvider
import java.io.IOException
import java.math.BigInteger
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Service
class Web3jBlockchainService(
    private val abiDecoderService: AbiDecoderService,
    private val uuidProvider: UuidProvider,
    private val utcDateTimeProvider: UtcDateTimeProvider,
    private val web3jBlockchainServiceCacheRepository: Web3jBlockchainServiceCacheRepository,
    applicationProperties: ApplicationProperties
) : BlockchainService {

    companion object : KLogging() {
        private data class BlockDescriptor(
            val blockNumber: BlockNumber,
            val blockConfirmations: BigInteger,
            val timestamp: UtcDateTime
        )

        private data class CachedBlockNumber(
            val blockNumber: BlockNumber,
            val cachedAt: UtcDateTime
        ) {
            fun shouldInvalidate(now: UtcDateTime, cacheDuration: Duration) =
                (cachedAt.value + cacheDuration).isBefore(now.value)
        }
    }

    private val chainHandler = ChainPropertiesHandler(applicationProperties)
    private val latestBlockCache = ConcurrentHashMap<ChainSpec, CachedBlockNumber>()

    override fun fetchAccountBalance(
        chainSpec: ChainSpec,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching account balance, chainSpec: $chainSpec, walletAddress: $walletAddress," +
                " blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val blockDescriptor = blockchainProperties.web3j.getBlockDescriptor(
            blockParameter = blockParameter,
            chainSpec = chainSpec,
            cacheDuration = blockchainProperties.latestBlockCacheDuration
        )

        return web3jBlockchainServiceCacheRepository.getCachedFetchAccountBalance(
            chainSpec = chainSpec,
            walletAddress = walletAddress,
            blockNumber = blockDescriptor.blockNumber
        ) ?: run {
            val balance = blockchainProperties.web3j.ethGetBalance(
                walletAddress.rawValue,
                blockDescriptor.blockNumber.toWeb3Parameter()
            ).sendSafely()?.balance?.let { Balance(it) }
                ?: throw BlockchainReadException("Unable to read balance of address: ${walletAddress.rawValue}")

            val accountBalance = AccountBalance(
                wallet = walletAddress,
                blockNumber = blockDescriptor.blockNumber,
                timestamp = blockDescriptor.timestamp,
                amount = balance
            )

            if (blockchainProperties.shouldCache(blockDescriptor.blockConfirmations)) {
                web3jBlockchainServiceCacheRepository.cacheFetchAccountBalance(
                    id = uuidProvider.getUuid(),
                    chainSpec = chainSpec,
                    accountBalance = accountBalance
                )
            }

            accountBalance
        }
    }

    override fun fetchErc20AccountBalance(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress,
        walletAddress: WalletAddress,
        blockParameter: BlockParameter
    ): AccountBalance {
        logger.debug {
            "Fetching ERC20 balance, chainSpec: $chainSpec, contractAddress: $contractAddress," +
                " walletAddress: $walletAddress, blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val blockDescriptor = blockchainProperties.web3j.getBlockDescriptor(
            blockParameter = blockParameter,
            chainSpec = chainSpec,
            cacheDuration = blockchainProperties.latestBlockCacheDuration
        )

        return web3jBlockchainServiceCacheRepository.getCachedFetchErc20AccountBalance(
            chainSpec = chainSpec,
            contractAddress = contractAddress,
            walletAddress = walletAddress,
            blockNumber = blockDescriptor.blockNumber
        ) ?: run {
            val contract = IERC20.load(
                contractAddress.rawValue,
                blockchainProperties.web3j,
                ReadonlyTransactionManager(blockchainProperties.web3j, contractAddress.rawValue),
                DefaultGasProvider()
            )

            contract.setDefaultBlockParameter(blockDescriptor.blockNumber.toWeb3Parameter())

            val accountBalance = contract.balanceOf(walletAddress.rawValue).sendSafely()
                ?.let {
                    AccountBalance(
                        walletAddress,
                        blockDescriptor.blockNumber,
                        blockDescriptor.timestamp,
                        Balance(it)
                    )
                } ?: throw BlockchainReadException(
                "Unable to read ERC20 contract at address: ${contractAddress.rawValue}" +
                    " on chain ID: ${chainSpec.chainId.value}"
            )

            if (blockchainProperties.shouldCache(blockDescriptor.blockConfirmations)) {
                web3jBlockchainServiceCacheRepository.cacheFetchErc20AccountBalance(
                    id = uuidProvider.getUuid(),
                    chainSpec = chainSpec,
                    contractAddress = contractAddress,
                    accountBalance = accountBalance
                )
            }

            accountBalance
        }
    }

    override fun fetchTransactionInfo(chainSpec: ChainSpec, txHash: TransactionHash): BlockchainTransactionInfo? {
        logger.debug { "Fetching transaction, chainSpec: $chainSpec, txHash: $txHash" }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val web3j = blockchainProperties.web3j

        return shortCircuiting {
            val currentBlockNumber = web3j.latestBlockNumber(chainSpec, blockchainProperties.latestBlockCacheDuration)

            web3jBlockchainServiceCacheRepository.getCachedFetchTransactionInfo(
                chainSpec = chainSpec,
                txHash = txHash,
                currentBlockNumber = currentBlockNumber
            ) ?: run {
                val transaction = web3j.ethGetTransactionByHash(txHash.value).sendSafely()
                    ?.transaction?.orElse(null).bind()
                val receipt = web3j.ethGetTransactionReceipt(txHash.value).sendSafely()
                    ?.transactionReceipt?.orElse(null).bind()
                val blockConfirmations = currentBlockNumber.value - transaction.blockNumber.bind()
                val txBlockNumber = transaction.blockNumber
                val timestamp = web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(txBlockNumber), false)
                    .sendSafely()?.block?.timestamp?.let { UtcDateTime.ofEpochSeconds(it.longValueExact()) }.bind()
                val txInfo = BlockchainTransactionInfo(
                    hash = TransactionHash(transaction.hash),
                    from = WalletAddress(transaction.from),
                    to = transaction.to?.let { WalletAddress(it) } ?: ZeroAddress.toWalletAddress(),
                    deployedContractAddress = receipt.contractAddress?.let { ContractAddress(it) },
                    data = FunctionData(transaction.input),
                    value = Balance(transaction.value),
                    blockConfirmations = blockConfirmations,
                    timestamp = timestamp,
                    success = receipt.isStatusOK
                )

                if (blockchainProperties.shouldCache(blockConfirmations)) {
                    web3jBlockchainServiceCacheRepository.cacheFetchTransactionInfo(
                        id = uuidProvider.getUuid(),
                        chainSpec = chainSpec,
                        txHash = txHash,
                        blockNumber = BlockNumber(txBlockNumber),
                        txInfo = txInfo
                    )
                }

                txInfo
            }
        }
    }

    override fun callReadonlyFunction(
        chainSpec: ChainSpec,
        params: ExecuteReadonlyFunctionCallParams,
        blockParameter: BlockParameter
    ): ReadonlyFunctionCallResult {
        logger.debug {
            "Executing read-only function call, chainSpec: $chainSpec, params: $params, blockParameter: $blockParameter"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val blockDescriptor = blockchainProperties.web3j.getBlockDescriptor(
            blockParameter = blockParameter,
            chainSpec = chainSpec,
            cacheDuration = blockchainProperties.latestBlockCacheDuration
        )
        val functionCallResponse = blockchainProperties.web3j.ethCall(
            Transaction.createEthCallTransaction(
                params.callerAddress.rawValue,
                params.contractAddress.rawValue,
                params.functionData.value
            ),
            blockDescriptor.blockNumber.toWeb3Parameter()
        ).sendSafely()?.value?.takeIf { it != "0x" }
            ?: throw BlockchainReadException(
                "Unable to call function ${params.functionName} on contract with address: ${params.contractAddress}"
            )
        val returnValues = abiDecoderService.decode(
            types = params.outputParams.map { it.deserializedType },
            encodedInput = functionCallResponse
        )

        return ReadonlyFunctionCallResult(
            blockNumber = blockDescriptor.blockNumber,
            timestamp = blockDescriptor.timestamp,
            returnValues = returnValues,
            rawReturnValue = functionCallResponse
        )
    }

    override fun findContractDeploymentTransaction(
        chainSpec: ChainSpec,
        contractAddress: ContractAddress
    ): ContractDeploymentTransactionInfo? {
        logger.debug {
            "Searching for contract deployment transaction, chainSpec: $chainSpec, contractAddress: $contractAddress"
        }
        val blockchainProperties = chainHandler.getBlockchainProperties(chainSpec)
        val web3j = blockchainProperties.web3j
        val currentBlockNumber = web3j.latestBlockNumber(chainSpec, blockchainProperties.latestBlockCacheDuration)

        val searchResult = BinarySearch(
            lowerBound = BigInteger.ZERO,
            upperBound = currentBlockNumber.value,
            getValue = { currentBlock ->
                web3j.ethGetTransactionCount(
                    contractAddress.rawValue,
                    DefaultBlockParameter.valueOf(currentBlock)
                ).sendSafely()?.transactionCount ?: throw TemporaryBlockchainReadException()
            },
            updateLowerBound = { txCount -> txCount == BigInteger.ZERO },
            updateUpperBound = { txCount -> txCount != BigInteger.ZERO }
        )
        val contractDeploymentBlock = listOf(searchResult, searchResult + BigInteger.ONE).find {
            web3j.ethGetTransactionCount(
                contractAddress.rawValue,
                DefaultBlockParameter.valueOf(it)
            ).sendSafely()?.transactionCount != BigInteger.ZERO
        }

        val deployTx = contractDeploymentBlock?.let {
            web3j.ethGetBlockByNumber(DefaultBlockParameter.valueOf(it), true).sendSafely()?.block?.transactions
        }
            ?.mapNotNull { it as? EthBlock.TransactionObject }
            ?.filter { it.to == null || it.to?.let { t -> WalletAddress(t) } == ZeroAddress.toWalletAddress() }
            ?.asSequence()
            ?.mapNotNull {
                web3j.ethGetTransactionReceipt(it.hash).sendSafely()?.transactionReceipt?.orElse(null)?.pairWith(it)
            }
            ?.find {
                it.first.isStatusOK && it.first.contractAddress?.let { ca -> ContractAddress(ca) } == contractAddress
            }
        val binary = web3j.ethGetCode(contractAddress.rawValue, currentBlockNumber.toWeb3Parameter()).sendSafely()?.code

        return deployTx?.let {
            binary?.let {
                ContractDeploymentTransactionInfo(
                    hash = TransactionHash(deployTx.first.transactionHash),
                    from = WalletAddress(deployTx.first.from),
                    deployedContractAddress = ContractAddress(deployTx.first.contractAddress),
                    data = FunctionData(deployTx.second.input),
                    value = Balance(deployTx.second.value),
                    binary = ContractBinaryData(binary)
                )
            }
        }
    }

    private fun Web3j.getBlockDescriptor(
        blockParameter: BlockParameter,
        chainSpec: ChainSpec,
        cacheDuration: Duration
    ): BlockDescriptor {
        val block = ethGetBlockByNumber(blockParameter.toWeb3Parameter(), false).sendSafely()?.block
        val blockNumber = block?.number?.let { BlockNumber(it) }
        val currentBlockNumber = latestBlockNumber(chainSpec, cacheDuration)
        val timestamp = block?.timestamp?.let { UtcDateTime.ofEpochSeconds(it.longValueExact()) }

        return if (blockNumber != null && timestamp != null) {
            BlockDescriptor(
                blockNumber = blockNumber,
                blockConfirmations = (currentBlockNumber.value - blockNumber.value).max(BigInteger.ZERO),
                timestamp = timestamp
            )
        } else {
            throw TemporaryBlockchainReadException()
        }
    }

    private fun Web3j.latestBlockNumber(chainSpec: ChainSpec, cacheDuration: Duration): BlockNumber {
        val now = utcDateTimeProvider.getUtcDateTime()
        val cachedBlockNumber = latestBlockCache[chainSpec]?.takeIf { it.shouldInvalidate(now, cacheDuration).not() }
            ?.blockNumber

        return if (cachedBlockNumber != null) {
            cachedBlockNumber
        } else {
            val ethLatestBlockNumber = ethBlockNumber().sendSafely()?.blockNumber?.let { BlockNumber(it) }
                ?: throw TemporaryBlockchainReadException()
            latestBlockCache[chainSpec] = CachedBlockNumber(ethLatestBlockNumber, now)
            ethLatestBlockNumber
        }
    }

    @Suppress("ReturnCount")
    private fun <S, T : Response<*>?> Request<S, T>.sendSafely(): T? {
        try {
            val value = this.send()
            if (value?.hasError() == true) {
                logger.warn { "Web3j call errors: ${value.error.message}" }
                return null
            }
            return value
        } catch (ex: IOException) {
            logger.warn("Failed blockchain call", ex)
            return null
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private fun <T> RemoteFunctionCall<T>.sendSafely(): T? =
        try {
            this.send()
        } catch (ex: Exception) {
            logger.warn("Failed smart contract call", ex)
            null
        }

    private fun <T, U> T.pairWith(that: U) = Pair(this, that)
}
