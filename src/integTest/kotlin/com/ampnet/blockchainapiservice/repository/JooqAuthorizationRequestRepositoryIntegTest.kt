package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.generated.jooq.enums.UserIdentifierType
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.AuthorizationRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ProjectRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.UserIdentifierRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreAuthorizationRequestParams
import com.ampnet.blockchainapiservice.model.result.AuthorizationRequest
import com.ampnet.blockchainapiservice.testcontainers.SharedTestContainers
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.SignedMessage
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
import java.util.UUID

@JooqTest
@Import(JooqAuthorizationRequestRepository::class)
@DirtiesContext
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqAuthorizationRequestRepositoryIntegTest : TestBase() {

    companion object {
        private const val REDIRECT_URL = "redirect-url"
        private val REQUESTED_WALLET_ADDRESS = WalletAddress("b")
        private val ARBITRARY_DATA = TestData.EMPTY_JSON_OBJECT
        private const val SCREEN_BEFORE_ACTION_MESSAGE = "before-action-message"
        private const val SCREEN_AFTER_ACTION_MESSAGE = "after-action-message"
        private val ACTUAL_WALLET_ADDRESS = WalletAddress("c")
        private val SIGNED_MESSAGE = SignedMessage("signed-message")
        private val PROJECT_ID = UUID.randomUUID()
        private val OWNER_ID = UUID.randomUUID()
    }

    @Suppress("unused")
    private val postgresContainer = SharedTestContainers.postgresContainer

    @Autowired
    private lateinit var repository: JooqAuthorizationRequestRepository

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
    fun mustCorrectlyFetchAuthorizationRequestById() {
        val id = UUID.randomUUID()

        suppose("some authorization request exists in database") {
            dslContext.executeInsert(
                AuthorizationRequestRecord(
                    id = id,
                    projectId = PROJECT_ID,
                    redirectUrl = REDIRECT_URL,
                    requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                    arbitraryData = ARBITRARY_DATA,
                    screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                    screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                    actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                    signedMessage = SIGNED_MESSAGE,
                    createdAt = TestData.TIMESTAMP
                )
            )
        }

        verify("authorization is correctly fetched by ID") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AuthorizationRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        redirectUrl = REDIRECT_URL,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentAuthorizationById() {
        verify("null is returned when fetching non-existent authorization request") {
            val result = repository.getById(UUID.randomUUID())

            assertThat(result).withMessage()
                .isNull()
        }
    }

    @Test
    fun mustCorrectlyFetchAuthorizationRequestsByProject() {
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
            AuthorizationRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                redirectUrl = REDIRECT_URL,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            AuthorizationRequestRecord(
                id = UUID.randomUUID(),
                projectId = PROJECT_ID,
                redirectUrl = REDIRECT_URL,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )
        val otherRequests = listOf(
            AuthorizationRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                redirectUrl = REDIRECT_URL,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            ),
            AuthorizationRequestRecord(
                id = UUID.randomUUID(),
                projectId = otherProjectId,
                redirectUrl = REDIRECT_URL,
                requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                arbitraryData = ARBITRARY_DATA,
                screenBeforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                screenAfterActionMessage = SCREEN_AFTER_ACTION_MESSAGE,
                actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                signedMessage = SIGNED_MESSAGE,
                createdAt = TestData.TIMESTAMP
            )
        )

        suppose("some authorization requests exist in database") {
            dslContext.batchInsert(projectRequests + otherRequests).execute()
        }

        verify("authorization requests are correctly fetched by project") {
            val result = repository.getAllByProjectId(PROJECT_ID)

            assertThat(result).withMessage()
                .containsExactlyInAnyOrderElementsOf(
                    projectRequests.map {
                        AuthorizationRequest(
                            id = it.id!!,
                            projectId = it.projectId!!,
                            redirectUrl = it.redirectUrl!!,
                            requestedWalletAddress = it.requestedWalletAddress,
                            actualWalletAddress = it.actualWalletAddress,
                            signedMessage = it.signedMessage,
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
    fun mustCorrectlyStoreAuthorizationRequest() {
        val id = UUID.randomUUID()
        val params = StoreAuthorizationRequestParams(
            id = id,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        val storedAuthorizationRequest = suppose("authorization request is stored in database") {
            repository.store(params)
        }

        val expectedAuthorizationRequest = AuthorizationRequest(
            id = id,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            actualWalletAddress = null,
            signedMessage = null,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        verify("storing authorization request returns correct result") {
            assertThat(storedAuthorizationRequest).withMessage()
                .isEqualTo(expectedAuthorizationRequest)
        }

        verify("authorization request was stored in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(expectedAuthorizationRequest)
        }
    }

    @Test
    fun mustCorrectlySetSignedMessageForAuthorizationRequestWithNullWalletAddressAndSignedMessage() {
        val id = UUID.randomUUID()
        val params = StoreAuthorizationRequestParams(
            id = id,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("authorization request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            assertThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE)).withMessage()
                .isTrue()
        }

        verify("walletAddress and signedMessage were correctly set in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AuthorizationRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        redirectUrl = REDIRECT_URL,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }

    @Test
    fun mustNotSetWalletAddressAndSignedMessageForAuthorizationRequestWhenWalletAddressAndSignedMessageAreAlreadySet() {
        val id = UUID.randomUUID()
        val params = StoreAuthorizationRequestParams(
            id = id,
            projectId = PROJECT_ID,
            redirectUrl = REDIRECT_URL,
            requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
            arbitraryData = ARBITRARY_DATA,
            screenConfig = ScreenConfig(
                beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
            ),
            createdAt = TestData.TIMESTAMP
        )

        suppose("authorization request is stored in database") {
            repository.store(params)
        }

        verify("setting walletAddress and signedMessage will succeed") {
            assertThat(repository.setSignedMessage(id, ACTUAL_WALLET_ADDRESS, SIGNED_MESSAGE)).withMessage()
                .isTrue()
        }

        verify("setting another walletAddress and signedMessage will not succeed") {
            assertThat(repository.setSignedMessage(id, WalletAddress("dead"), SignedMessage("another-message")))
                .withMessage()
                .isFalse()
        }

        verify("first walletAddress and signedMessage remain in database") {
            val result = repository.getById(id)

            assertThat(result).withMessage()
                .isEqualTo(
                    AuthorizationRequest(
                        id = id,
                        projectId = PROJECT_ID,
                        redirectUrl = REDIRECT_URL,
                        requestedWalletAddress = REQUESTED_WALLET_ADDRESS,
                        actualWalletAddress = ACTUAL_WALLET_ADDRESS,
                        signedMessage = SIGNED_MESSAGE,
                        arbitraryData = ARBITRARY_DATA,
                        screenConfig = ScreenConfig(
                            beforeActionMessage = SCREEN_BEFORE_ACTION_MESSAGE,
                            afterActionMessage = SCREEN_AFTER_ACTION_MESSAGE
                        ),
                        createdAt = TestData.TIMESTAMP
                    )
                )
        }
    }
}
