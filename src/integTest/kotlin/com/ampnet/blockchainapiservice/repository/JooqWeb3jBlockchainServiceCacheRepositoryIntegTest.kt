package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.blockchain.properties.ChainSpec
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.AccountBalance
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.ZeroAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import
import org.springframework.test.annotation.DirtiesContext
import java.math.BigInteger
import java.util.UUID

@JooqTest
@Import(JooqWeb3jBlockchainServiceCacheRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqWeb3jBlockchainServiceCacheRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_SPEC = ChainSpec(
            chainId = ChainId(123L),
            customRpcUrl = null
        )
        private val BLOCK_NUMBER = BlockNumber(BigInteger.valueOf(456L))
        private val ACCOUNT_BALANCE = AccountBalance(
            wallet = WalletAddress("abc"),
            blockNumber = BLOCK_NUMBER,
            timestamp = TestData.TIMESTAMP,
            amount = Balance(BigInteger.valueOf(1000L))
        )
        private val CONTRACT_ADDRESS = ContractAddress("def")
        private val TX_INFO = BlockchainTransactionInfo(
            hash = TransactionHash("tx-hash"),
            from = WalletAddress("123"),
            to = ZeroAddress.toWalletAddress(),
            deployedContractAddress = CONTRACT_ADDRESS,
            data = FunctionData("1234"),
            value = Balance(BigInteger.TEN),
            blockConfirmations = BigInteger.TEN,
            timestamp = TestData.TIMESTAMP,
            success = true
        )
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqWeb3jBlockchainServiceCacheRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    fun mustCorrectlyCacheFetchAccountBalance() {
        val id = UUID.randomUUID()

        suppose("fetchAccountBalance call will be cached") {
            repository.cacheFetchAccountBalance(
                id = id,
                chainSpec = CHAIN_SPEC,
                accountBalance = ACCOUNT_BALANCE
            )
        }

        verify("fetchAccountBalance call is correctly cached") {
            assertThat(
                repository.getCachedFetchAccountBalance(
                    chainSpec = CHAIN_SPEC,
                    walletAddress = ACCOUNT_BALANCE.wallet,
                    blockNumber = ACCOUNT_BALANCE.blockNumber
                )
            ).withMessage()
                .isEqualTo(ACCOUNT_BALANCE)
        }
    }

    @Test
    fun mustCorrectlyCacheFetchErc20AccountBalance() {
        val id = UUID.randomUUID()

        suppose("fetchErc20AccountBalance call will be cached") {
            repository.cacheFetchErc20AccountBalance(
                id = id,
                chainSpec = CHAIN_SPEC,
                contractAddress = CONTRACT_ADDRESS,
                accountBalance = ACCOUNT_BALANCE
            )
        }

        verify("fetchErc20AccountBalance call is correctly cached") {
            assertThat(
                repository.getCachedFetchErc20AccountBalance(
                    chainSpec = CHAIN_SPEC,
                    contractAddress = CONTRACT_ADDRESS,
                    walletAddress = ACCOUNT_BALANCE.wallet,
                    blockNumber = ACCOUNT_BALANCE.blockNumber
                )
            ).withMessage()
                .isEqualTo(ACCOUNT_BALANCE)
        }
    }

    @Test
    fun mustCorrectlyCacheFetchTransactionInfo() {
        val id = UUID.randomUUID()

        suppose("fetchTransactionInfo call will be cached") {
            repository.cacheFetchTransactionInfo(
                id = id,
                chainSpec = CHAIN_SPEC,
                txHash = TX_INFO.hash,
                blockNumber = BlockNumber(BLOCK_NUMBER.value - TX_INFO.blockConfirmations),
                txInfo = TX_INFO
            )
        }

        verify("fetchTransactionInfo call is correctly cached") {
            assertThat(
                repository.getCachedFetchTransactionInfo(
                    chainSpec = CHAIN_SPEC,
                    txHash = TX_INFO.hash,
                    currentBlockNumber = BLOCK_NUMBER
                )
            ).withMessage()
                .isEqualTo(TX_INFO)
        }
    }
}
