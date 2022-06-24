package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.model.result.ClientInfo
import com.ampnet.blockchainapiservice.testcontainers.PostgresTestContainer
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jooq.JooqTest
import org.springframework.context.annotation.Import

@JooqTest
@Import(JooqClientInfoRepository::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class JooqClientInfoRepositoryIntegTest : TestBase() {

    @Suppress("unused")
    private val postgresContainer = PostgresTestContainer()

    @Autowired
    private lateinit var repository: JooqClientInfoRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @Test
    fun mustCorrectlyFetchClientInfoByClientId() {
        val clientInfo = ClientInfo(
            clientId = "example-client",
            chainId = ChainId(1337L),
            sendRedirectUrl = "example-send-url",
            balanceRedirectUrl = "example-balance-url",
            lockRedirectUrl = "example-lock-url",
            tokenAddress = ContractAddress("abc")
        )

        suppose("some client info exists in database") {
            dslContext.executeInsert(
                ClientInfoRecord(
                    clientId = clientInfo.clientId,
                    chainId = clientInfo.chainId.alternative,
                    sendRedirectUrl = clientInfo.sendRedirectUrl.alternative,
                    balanceRedirectUrl = clientInfo.balanceRedirectUrl.alternative,
                    lockRedirectUrl = clientInfo.lockRedirectUrl.alternative,
                    tokenAddress = clientInfo.tokenAddress.alternative
                )
            )
        }

        verify("client info is correctly fetched by ID") {
            val result = repository.getById(clientInfo.clientId)

            assertThat(result).withMessage()
                .isEqualTo(clientInfo)
        }
    }

    @Test
    fun mustReturnNullWhenFetchingNonExistentClientInfoByClientId() {
        verify("null is returned when fetching non-existent client info") {
            val result = repository.getById("non-existent-client-id")

            assertThat(result).withMessage()
                .isNull()
        }
    }
}