package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.blockchain.SimpleERC20
import com.ampnet.blockchainapiservice.blockchain.properties.Chain
import com.ampnet.blockchainapiservice.config.binding.RpcUrlSpecResolver
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.generated.jooq.tables.ClientInfoTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.SendErc20RequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ClientInfoRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.StoreSendErc20RequestParams
import com.ampnet.blockchainapiservice.model.response.SendErc20RequestResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.SendErc20Request
import com.ampnet.blockchainapiservice.repository.SendErc20RequestRepository
import com.ampnet.blockchainapiservice.testcontainers.HardhatTestContainer
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.DSLContext
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import org.web3j.tx.gas.DefaultGasProvider
import java.math.BigInteger
import java.util.UUID

class SendErc20RequestControllerApiTest : ControllerTestBase() {

    private val accounts = HardhatTestContainer.accounts

    @Autowired
    private lateinit var sendErc20RequestRepository: SendErc20RequestRepository

    @Autowired
    private lateinit var dslContext: DSLContext

    @BeforeEach
    fun beforeEach() {
        dslContext.deleteFrom(ClientInfoTable.CLIENT_INFO).execute()
        dslContext.deleteFrom(SendErc20RequestTable.SEND_ERC20_REQUEST).execute()
    }

    @Test
    fun mustCorrectlyCreateSendErc20RequestViaClientId() {
        val clientId = "client-id"
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("cafebabe")

        suppose("some client info exists in database") {
            dslContext.insertInto(ClientInfoTable.CLIENT_INFO)
                .set(
                    ClientInfoRecord(
                        clientId = "client-id",
                        chainId = chainId.value,
                        sendRedirectUrl = redirectUrl,
                        balanceRedirectUrl = null,
                        tokenAddress = tokenAddress.rawValue
                    )
                )
                .execute()
        }

        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create send ERC20 request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "$clientId",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    SendErc20RequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null
                        )
                    )
                )
        }

        verify("send ERC20 request is correctly stored in database") {
            val storedRequest = sendErc20RequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    SendErc20Request(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyCreateSendErc20RequestViaChainIdRedirectUrlAndTokenAddress() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        val response = suppose("request to create send ERC20 request is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    SendErc20RequestResponse(
                        id = response.id,
                        status = Status.PENDING,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = null,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = response.sendTx.data,
                            blockConfirmations = null
                        )
                    )
                )
        }

        verify("send ERC20 request is correctly stored in database") {
            val storedRequest = sendErc20RequestRepository.getById(response.id)

            assertThat(storedRequest).withMessage()
                .isEqualTo(
                    SendErc20Request(
                        id = response.id,
                        chainId = chainId,
                        redirectUrl = redirectUrl.replace("\${id}", response.id.toString()),
                        tokenAddress = tokenAddress,
                        tokenAmount = amount,
                        tokenSenderAddress = senderAddress,
                        tokenRecipientAddress = recipientAddress,
                        txHash = null,
                        arbitraryData = response.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        )
                    )
                )
        }
    }

    @Test
    fun mustReturn400BadRequestForNonExistentClientId() {
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for non-existent clientId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "client_id": "non-existent-client-id",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.NON_EXISTENT_CLIENT_ID)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingChainId() {
        val redirectUrl = "https://example.com"
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing chainId") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingRedirectUrl() {
        val chainId = Chain.HARDHAT_TESTNET.id
        val tokenAddress = ContractAddress("a")
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustReturn400BadRequestForMissingTokenAddress() {
        val redirectUrl = "https://example.com"
        val chainId = Chain.HARDHAT_TESTNET.id
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress("b")
        val recipientAddress = WalletAddress("c")

        verify("400 is returned for missing redirectUrl") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.INCOMPLETE_REQUEST)
        }
    }

    @Test
    fun mustCorrectlyFetchSendErc20Request() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create send ERC20 request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made with missing transaction data") {
            contract.transferAndMine(recipientAddress.rawValue, amount.rawValue)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to send ERC20 request") {
            sendErc20RequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch send ERC20 request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/send/${createResponse.id}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    SendErc20RequestResponse(
                        id = createResponse.id,
                        status = Status.FAILED,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", createResponse.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations
                        )
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustCorrectlyFetchSendErc20RequestWhenCustomRpcUrlIsSpecified() {
        val mainAccount = accounts[0]

        val contract = suppose("simple ERC20 contract is deployed") {
            SimpleERC20.deploy(
                hardhatContainer.web3j,
                mainAccount,
                DefaultGasProvider(),
                listOf(mainAccount.address),
                listOf(BigInteger("10000")),
                mainAccount.address
            ).sendAndMine()
        }

        val chainId = Chain.HARDHAT_TESTNET.id
        val redirectUrl = "https://example.com/\${id}"
        val tokenAddress = ContractAddress(contract.contractAddress)
        val amount = Balance(BigInteger.TEN)
        val senderAddress = WalletAddress(mainAccount.address)
        val recipientAddress = WalletAddress(accounts[1].address)

        val createResponse = suppose("request to create send ERC20 request is made") {
            val createResponse = mockMvc.perform(
                MockMvcRequestBuilders.post("/send")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "chain_id": ${chainId.value},
                                "redirect_url": "$redirectUrl",
                                "token_address": "${tokenAddress.rawValue}",
                                "amount": "${amount.rawValue}",
                                "sender_address": "${senderAddress.rawValue}",
                                "recipient_address": "${recipientAddress.rawValue}",
                                "arbitrary_data": {
                                    "test": true
                                },
                                "screen_config": {
                                    "title": "title",
                                    "message": "message",
                                    "logo": "logo"
                                }
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(createResponse.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        val txHash = suppose("some ERC20 transfer transaction is made with missing transaction data") {
            contract.transferAndMine(recipientAddress.rawValue, amount.rawValue)
                ?.get()?.transactionHash?.let { TransactionHash(it) }!!
        }

        suppose("transaction will have at least one block confirmation") {
            hardhatContainer.waitAndMine()
        }

        suppose("transaction hash is attached to send ERC20 request") {
            sendErc20RequestRepository.setTxHash(createResponse.id, txHash)
        }

        val fetchResponse = suppose("request to fetch send ERC20 request is made") {
            val fetchResponse = mockMvc.perform(
                MockMvcRequestBuilders.get("/send/${createResponse.id}")
                    .header(
                        RpcUrlSpecResolver.RPC_URL_OVERRIDE_HEADER,
                        "http://localhost:${hardhatContainer.mappedPort}"
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(fetchResponse.response.contentAsString, SendErc20RequestResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(fetchResponse).withMessage()
                .isEqualTo(
                    SendErc20RequestResponse(
                        id = createResponse.id,
                        status = Status.FAILED,
                        chainId = chainId.value,
                        tokenAddress = tokenAddress.rawValue,
                        amount = amount.rawValue,
                        senderAddress = senderAddress.rawValue,
                        recipientAddress = recipientAddress.rawValue,
                        arbitraryData = createResponse.arbitraryData,
                        screenConfig = ScreenConfig(
                            title = "title",
                            message = "message",
                            logo = "logo"
                        ),
                        redirectUrl = redirectUrl.replace("\${id}", createResponse.id.toString()),
                        sendTx = TransactionResponse(
                            txHash = txHash.value,
                            from = senderAddress.rawValue,
                            to = tokenAddress.rawValue,
                            data = createResponse.sendTx.data,
                            blockConfirmations = fetchResponse.sendTx.blockConfirmations
                        )
                    )
                )

            assertThat(fetchResponse.sendTx.blockConfirmations)
                .isNotZero()
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentSendErc20Request() {
        verify("404 is returned for non-existent send ERC20 request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/send/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionHash() {
        val id = UUID.randomUUID()

        suppose("some send ERC20 request without transaction hash exists in database") {
            sendErc20RequestRepository.store(
                StoreSendErc20RequestParams(
                    id = id,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    tokenSenderAddress = WalletAddress("b"),
                    tokenRecipientAddress = WalletAddress("c"),
                    arbitraryData = null,
                    screenConfig = ScreenConfig(
                        title = "title",
                        message = "message",
                        logo = "logo"
                    )
                )
            )
        }

        val txHash = TransactionHash("tx-hash")

        suppose("request to attach transaction hash to send ERC20 request is made") {
            mockMvc.perform(
                MockMvcRequestBuilders.put("/send/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "${txHash.value}"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()
        }

        verify("transaction hash is correctly attached to send ERC20 request") {
            val storedRequest = sendErc20RequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }

    @Test
    fun mustReturn400BadRequestWhenTransactionHashIsNotAttached() {
        val id = UUID.randomUUID()
        val txHash = TransactionHash("tx-hash")

        suppose("some send ERC20 request with transaction hash exists in database") {
            sendErc20RequestRepository.store(
                StoreSendErc20RequestParams(
                    id = id,
                    chainId = Chain.HARDHAT_TESTNET.id,
                    redirectUrl = "https://example.com/$id",
                    tokenAddress = ContractAddress("a"),
                    tokenAmount = Balance(BigInteger.TEN),
                    tokenSenderAddress = WalletAddress("b"),
                    tokenRecipientAddress = WalletAddress("c"),
                    arbitraryData = null,
                    screenConfig = ScreenConfig(
                        title = "title",
                        message = "message",
                        logo = "logo"
                    )
                )
            )
            sendErc20RequestRepository.setTxHash(id, txHash)
        }

        verify("400 is returned when attaching transaction hash") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.put("/send/$id")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(
                        """
                            {
                                "tx_hash": "different-tx-hash"
                            }
                        """.trimIndent()
                    )
            )
                .andExpect(MockMvcResultMatchers.status().isBadRequest)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.TX_HASH_ALREADY_SET)
        }

        verify("transaction hash is not changed in database") {
            val storedRequest = sendErc20RequestRepository.getById(id)

            assertThat(storedRequest?.txHash)
                .isEqualTo(txHash)
        }
    }
}
