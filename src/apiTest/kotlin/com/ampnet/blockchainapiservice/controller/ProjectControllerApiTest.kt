package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.model.response.ApiKeyResponse
import com.ampnet.blockchainapiservice.model.response.ProjectResponse
import com.ampnet.blockchainapiservice.model.response.ProjectsResponse
import com.ampnet.blockchainapiservice.model.result.ApiKey
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.repository.ApiKeyRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.repository.UserIdentifierRepository
import com.ampnet.blockchainapiservice.security.WithMockUser
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.UtcDateTime
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.time.OffsetDateTime
import java.util.UUID

class ProjectControllerApiTest : ControllerTestBase() {

    @Autowired
    private lateinit var userIdentifierRepository: UserIdentifierRepository

    @Autowired
    private lateinit var projectRepository: ProjectRepository

    @Autowired
    private lateinit var apiKeyRepository: ApiKeyRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        postgresContainer.cleanAllDatabaseTables(dslContext)
    }

    @Test
    @WithMockUser
    fun mustCorrectlyCreateProject() {
        val issuerContractAddress = ContractAddress("a")
        val baseRedirectUrl = BaseUrl("base-redirect-url")
        val chainId = Chain.HARDHAT_TESTNET.id
        val customRpcUrl = "custom-rpc-url"

        val response = suppose("request to create project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "issuer_contract_address": "${issuerContractAddress.rawValue}",
                                "base_redirect_url": "${baseRedirectUrl.value}",
                                "chain_id": ${chainId.value},
                                "custom_rpc_url": "$customRpcUrl"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ProjectResponse(
                        id = response.id,
                        ownerId = response.ownerId,
                        issuerContractAddress = issuerContractAddress.rawValue,
                        baseRedirectUrl = baseRedirectUrl.value,
                        chainId = chainId.value,
                        customRpcUrl = customRpcUrl,
                        createdAt = response.createdAt
                    )
                )
        }

        verify("user is correctly stored into the database") {
            assertThat(userIdentifierRepository.getById(response.ownerId)).withMessage()
                .isEqualTo(
                    UserWalletAddressIdentifier(
                        id = response.ownerId,
                        walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
                    )
                )
        }

        verify("project is correctly stored into the database") {
            assertThat(projectRepository.getById(response.id))
                .isEqualTo(
                    Project(
                        id = response.id,
                        ownerId = response.ownerId,
                        issuerContractAddress = issuerContractAddress,
                        baseRedirectUrl = baseRedirectUrl,
                        chainId = chainId,
                        customRpcUrl = customRpcUrl,
                        createdAt = UtcDateTime(response.createdAt)
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectById() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val response = suppose("request to fetch project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ProjectResponse(
                        id = project.id,
                        ownerId = project.ownerId,
                        issuerContractAddress = project.issuerContractAddress.rawValue,
                        baseRedirectUrl = project.baseRedirectUrl.value,
                        chainId = project.chainId.value,
                        customRpcUrl = project.customRpcUrl,
                        createdAt = project.createdAt.value
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonExistentProjectId() {
        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonOwnedProjectId() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("0cafe0babe")
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectByIssuer() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val response = suppose("request to fetch project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/projects/by-chain/${project.chainId.value}/by-issuer/${project.issuerContractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ProjectResponse(
                        id = project.id,
                        ownerId = project.ownerId,
                        issuerContractAddress = project.issuerContractAddress.rawValue,
                        baseRedirectUrl = project.baseRedirectUrl.value,
                        chainId = project.chainId.value,
                        customRpcUrl = project.customRpcUrl,
                        createdAt = project.createdAt.value
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonExistentProjectIssuer() {
        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/by-chain/0/by-issuer/${ContractAddress("dead").rawValue}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForNonOwnedProjectIssuer() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("0cafe0babe")
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/projects/by-chain/${project.chainId.value}/by-issuer/${project.issuerContractAddress.rawValue}"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnAllProjectsForSomeUser() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project1 = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )
        val project2 = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("b"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some projects exist in database") {
            projectRepository.store(project1)
            projectRepository.store(project2)
        }

        val response = suppose("request to fetch project is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ProjectsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ProjectsResponse(
                        listOf(
                            ProjectResponse(
                                id = project1.id,
                                ownerId = project1.ownerId,
                                issuerContractAddress = project1.issuerContractAddress.rawValue,
                                baseRedirectUrl = project1.baseRedirectUrl.value,
                                chainId = project1.chainId.value,
                                customRpcUrl = project1.customRpcUrl,
                                createdAt = project1.createdAt.value
                            ),
                            ProjectResponse(
                                id = project2.id,
                                ownerId = project2.ownerId,
                                issuerContractAddress = project2.issuerContractAddress.rawValue,
                                baseRedirectUrl = project2.baseRedirectUrl.value,
                                chainId = project2.chainId.value,
                                customRpcUrl = project2.customRpcUrl,
                                createdAt = project2.createdAt.value
                            )
                        )
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyReturnProjectApiKey() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val apiKey = ApiKey(
            id = UUID.randomUUID(),
            projectId = project.id,
            apiKey = "api-key",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some API key exists in database") {
            apiKeyRepository.store(apiKey)
        }

        val response = suppose("request to fetch API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiKeyResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ApiKeyResponse(
                        id = apiKey.id,
                        projectId = apiKey.projectId,
                        apiKey = apiKey.apiKey,
                        createdAt = apiKey.createdAt.value
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustReturn404NotFoundForProjectWithoutApiKey() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-existent project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/projects/${project.id}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    @WithMockUser
    fun mustCorrectlyCreateApiKeyForSomeProject() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val response = suppose("request to create API key is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ApiKeyResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ApiKeyResponse(
                        id = response.id,
                        projectId = project.id,
                        apiKey = response.apiKey,
                        createdAt = response.createdAt
                    )
                )
        }

        verify("API key is correctly stored into the database") {
            assertThat(apiKeyRepository.getById(response.id))
                .isEqualTo(
                    ApiKey(
                        id = response.id,
                        projectId = response.projectId,
                        apiKey = response.apiKey,
                        createdAt = UtcDateTime(response.createdAt)
                    )
                )
        }
    }

    @Test
    @WithMockUser
    fun mustReturn400BadRequestWhenCreatingAnotherApiKeyForSomeProject() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        val apiKey = ApiKey(
            id = UUID.randomUUID(),
            projectId = project.id,
            apiKey = "api-key",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some API key exists in database") {
            apiKeyRepository.store(apiKey)
        }

        verify("400 is returned for project which already has API key") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.API_KEY_ALREADY_EXISTS)
        }
    }

    @Test
    @WithMockUser(address = HardhatTestContainer.ACCOUNT_ADDRESS_2)
    fun mustReturn404NotFoundWhenCreatingApiKeyByNonProjectOwner() {
        val userIdentifier = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress(HardhatTestContainer.ACCOUNT_ADDRESS_1)
        )

        suppose("some user identifier exists in database") {
            userIdentifierRepository.store(userIdentifier)
        }

        val project = Project(
            id = UUID.randomUUID(),
            ownerId = userIdentifier.id,
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = Chain.HARDHAT_TESTNET.id,
            customRpcUrl = "custom-rpc-url",
            createdAt = UtcDateTime(OffsetDateTime.now())
        )

        suppose("some project exists in database") {
            projectRepository.store(project)
        }

        verify("404 is returned for non-owned project") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/v1/projects/${project.id}/api-key")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }
}
