package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.Erc20LockRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreErc20LockRequestParams
import com.ampnet.blockchainapiservice.model.result.Erc20LockRequest
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.DurationSeconds
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
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
@Import(JooqErc20LockRequestRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqErc20LockRequestRepositoryIntegTest : TestBase() {

    companion object {
        private val CHAIN_ID = ChainId(1337L)
        private const val REDIRECT_URL = "redirect-url"
        private val TOKEN_ADDRESS = ContractAddress("a")
        private val TOKEN_AMOUNT = Balance(BigInteger.valueOf(123456L))
        private val LOCK_DURATION = DurationSeconds(BigInteger.valueOf(123L))
        private val LOCK_CONTRACT_ADDRESS = ContractAddress("b")
        private val TOKEN_SENDER_ADDRESS = WalletAddress("c")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val LOCK_SCREEN_BEFORE_ACTION_MESSAGE = "lock-screen-before-action-message"
        private const val LOCK_SCREEN_AFTER_ACTION_MESSAGE = "lock-screen-after-action-message"
        private val TX_HASH = TransactionHash("tx-hash")
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqErc20LockRequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)

        dslContext.executeInsert(
            UserIdentifierRecord(
                id = OWNER_ID,
                userIdentifier = "user-identifier",
                identifierType = UserIdentifierType.ETH_WALLET_ADDRESS
            )
        )

        dslContext.executeInsert(
            ProjectRecord(
                id = PROJECT_ID,
                ownerId = OWNER_ID,
                issuerContractAddress = ContractAddress("0"),
                baseRedirectUrl = BaseUrl("base-redirect-url"),
                chainId = ChainId(1337L),
                customRpcUrl = "custom-rpc-url",
                createdAt = TestData.TIMESTAMP
            )
        )
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestById() {
        val id = UUID.randomUUID()

        suppose("some ERC20 lock request exists in database") {
            dslContext.executeInsert(
                Erc20LockRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    chainId = CHAIN_ID,
                    redirectUrl = REDIRECT_URL,
                    tokenAddress = TOKEN_ADDRESS,
                    tokenAmount = TOKEN_AMOUNT,
                    lockDurationSeconds = LOCK_DURATION,
                    lockContractAddress = LOCK_CONTRACT_ADDRESS,
                    tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                    txHash = TX_HASH,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("ERC20 lock request is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        lockDuration = LOCK_DURATION,
                        lockContractAddress = LOCK_CONTRACT_ADDRESS,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentErc20LockRequestById() {
        verify("null is returned when fetching non-existent ERC20 lock request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchErc20LockRequestsByProject() {
        val otherProjectId = UUID.randomUUID()

        suppose("some other project is in database") {
            dslContext.executeInsert(
                ProjectRecord(
                    id = otherProjectId,
                    ownerId = OWNER_ID,
                    issuerContractAddress = ContractAddress("1"),
                    baseRedirectUrl = BaseUrl("base-redirect-url"),
                    chainId = ChainId(1337L),
                    customRpcUrl = "custom-rpc-url",
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        val projectRequests = listOf(
            Erc20LockRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                lockDurationSeconds = LOCK_DURATION,
                lockContractAddress = LOCK_CONTRACT_ADDRESS,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            Erc20LockRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                lockDurationSeconds = LOCK_DURATION,
                lockContractAddress = LOCK_CONTRACT_ADDRESS,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            Erc20LockRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                lockDurationSeconds = LOCK_DURATION,
                lockContractAddress = LOCK_CONTRACT_ADDRESS,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            ),
            Erc20LockRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                chainId = CHAIN_ID,
                redirectUrl = REDIRECT_URL,
                tokenAddress = TOKEN_ADDRESS,
                tokenAmount = TOKEN_AMOUNT,
                lockDurationSeconds = LOCK_DURATION,
                lockContractAddress = LOCK_CONTRACT_ADDRESS,
                tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE,
                txHash = TX_HASH,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some ERC20 lock requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("ERC20 lock requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        Erc20LockRequest(
                            id = it.id!!,
                            projectId = it.projectId!!,
                            chainId = it.chainId!!,
                            redirectUrl = it.redirectUrl!!,
                            tokenAddress = it.tokenAddress!!,
                            tokenAmount = it.tokenAmount!!,
                            lockDuration = it.lockDurationSeconds!!,
                            lockContractAddress = it.lockContractAddress!!,
                            tokenSenderAddress = it.tokenSenderAddress,
                            txHash = it.txHash,
                            arbitraryData = it.arbitraryData,
                            screenConfig = ScreenConfig(
                                beforeActionMessage = it.screenBeforeActionMessage,
                                afterActionMessage = it.screenAfterActionMessage
                            ),
                            createdAt = it.createdAt!!
                        )
                    }
                )
        }
    }

    @Test
    fun mustCorrectlyStoreErc20LockRequest() {
        val id = UUID.randomUUID()
        val params = StoreErc20LockRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            lockDuration = LOCK_DURATION,
            lockContractAddress = LOCK_CONTRACT_ADDRESS,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedErc20LockRequest = suppose("ERC20 lock request is stored in database") {
            repository.store(params)
        }

        val expectedErc20LockRequest = Erc20LockRequest(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            lockDuration = LOCK_DURATION,
            lockContractAddress = LOCK_CONTRACT_ADDRESS,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            txHash = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        verify("storing ERC20 lock request returns correct result") {
            assertThat(storedErc20LockRequest).withMessage()
                .isEqualTo(expectedErc20LockRequest)
        }

        verify("ERC20 lock request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedErc20LockRequest)
        }
    }

    @Test
    fun mustCorrectlySetTxInfoForErc20LockRequestWithNullTxHash() {
        val id = UUID.randomUUID()
        val params = StoreErc20LockRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            lockDuration = LOCK_DURATION,
            lockContractAddress = LOCK_CONTRACT_ADDRESS,
            tokenSenderAddress = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 lock request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, TOKEN_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("txInfo is correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        lockDuration = LOCK_DURATION,
                        lockContractAddress = LOCK_CONTRACT_ADDRESS,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotUpdateTokenSenderAddressForErc20LockRequestWhenTokenSenderIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20LockRequestParams(
            id = id,
            chainId = CHAIN_ID,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            lockDuration = LOCK_DURATION,
            lockContractAddress = LOCK_CONTRACT_ADDRESS,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 lock request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            val ignoredTokenSender = WalletAddress("f")
            assertThat(repository.setTxInfo(id, TX_HASH, ignoredTokenSender)).withMessage()
                .isTrue()
        }

        verify("txHash was correctly set while token sender was not updated") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        lockDuration = LOCK_DURATION,
                        lockContractAddress = LOCK_CONTRACT_ADDRESS,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetTxHashForErc20LockRequestWhenTxHashIsAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreErc20LockRequestParams(
            id = id,
            projectId = PROJECT_ID,
            chainId = CHAIN_ID,
            redirectUrl = REDIRECT_URL,
            tokenAddress = TOKEN_ADDRESS,
            tokenAmount = TOKEN_AMOUNT,
            lockDuration = LOCK_DURATION,
            lockContractAddress = LOCK_CONTRACT_ADDRESS,
            tokenSenderAddress = TOKEN_SENDER_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("ERC20 lock request is stored in database") {
            repository.store(params)
        }

        verify("setting txInfo will succeed") {
            assertThat(repository.setTxInfo(id, TX_HASH, TOKEN_SENDER_ADDRESS)).withMessage()
                .isTrue()
        }

        verify("setting another txInfo will not succeed") {
            assertThat(
                repository.setTxInfo(
                    id,
                    TransactionHash("different-tx-hash"),
                    TOKEN_SENDER_ADDRESS
                )
            ).withMessage().isFalse()
        }

        verify("first txHash remains in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    Erc20LockRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        chainId = CHAIN_ID,
                        redirectUrl = REDIRECT_URL,
                        tokenAddress = TOKEN_ADDRESS,
                        tokenAmount = TOKEN_AMOUNT,
                        lockDuration = LOCK_DURATION,
                        lockContractAddress = LOCK_CONTRACT_ADDRESS,
                        tokenSenderAddress = TOKEN_SENDER_ADDRESS,
                        txHash = TX_HASH,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = LOCK_SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = LOCK_SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
