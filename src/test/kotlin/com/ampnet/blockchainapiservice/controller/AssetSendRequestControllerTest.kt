package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.params.CreateAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateAssetSendRequest
import com.ampnet.blockchainapiservice.model.response.AssetSendRequestResponse
import com.ampnet.blockchainapiservice.model.response.AssetSendRequestsResponse
import com.ampnet.blockchainapiservice.model.response.TransactionResponse
import com.ampnet.blockchainapiservice.model.result.AssetSendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AssetSendRequestService
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.BaseUrl
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionData
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithTransactionData
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class AssetSendRequestControllerTest : TestBase() {

    @Test
    fun mustCorrectlyCreateAssetSendRequest() {
        val params = CreateAssetSendRequestParams(
            redirectUrl = "redirect-url",
            tokenAddress = ContractAddress("a"),
            assetAmount = Balance(BigInteger.TEN),
            assetSenderAddress = WalletAddress("b"),
            assetRecipientAddress = WalletAddress("c"),
            arbitraryData = TestData.EMPTY_JSON_OBJECT,
            screenConfig = ScreenConfig(
                beforeActionMessage = "before-action-message",
                afterActionMessage = "after-action-message"
            )
        )
        val result = AssetSendRequest(
            id = UUID.randomUUID(),
            projectId = UUID.randomUUID(),
            chainId = ChainId(1337L),
            redirectUrl = params.redirectUrl!!,
            tokenAddress = params.tokenAddress,
            assetAmount = params.assetAmount,
            assetSenderAddress = params.assetSenderAddress,
            assetRecipientAddress = params.assetRecipientAddress,
            txHash = null,
            arbitraryData = params.arbitraryData,
            screenConfig = params.screenConfig,
            createdAt = TestData.TIMESTAMP
        )
        val project = Project(
            id = result.projectId,
            ownerId = UUID.randomUUID(),
            issuerContractAddress = ContractAddress("a"),
            baseRedirectUrl = BaseUrl("base-redirect-url"),
            chainId = ChainId(1337L),
            customRpcUrl = "custom-rpc-url",
            createdAt = TestData.TIMESTAMP
        )
        val data = FunctionData("data")
        val service = mock<AssetSendRequestService>()

        suppose("asset send request will be created") {
            given(service.createAssetSendRequest(params, project))
                .willReturn(WithFunctionDataOrEthValue(result, data, null))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val request = CreateAssetSendRequest(
                redirectUrl = params.redirectUrl,
                tokenAddress = params.tokenAddress?.rawValue,
                assetType = AssetType.TOKEN,
                amount = params.assetAmount.rawValue,
                senderAddress = params.assetSenderAddress?.rawValue,
                recipientAddress = params.assetRecipientAddress.rawValue,
                arbitraryData = params.arbitraryData,
                screenConfig = params.screenConfig
            )
            val response = controller.createAssetSendRequest(project, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestResponse(
                            id = result.id,
                            projectId = project.id,
                            status = Status.PENDING,
                            chainId = result.chainId.value,
                            tokenAddress = result.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            amount = result.assetAmount.rawValue,
                            senderAddress = result.assetSenderAddress?.rawValue,
                            recipientAddress = result.assetRecipientAddress.rawValue,
                            arbitraryData = result.arbitraryData,
                            screenConfig = result.screenConfig,
                            redirectUrl = result.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = null,
                                from = result.assetSenderAddress?.rawValue,
                                to = result.tokenAddress!!.rawValue,
                                data = data.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = null,
                                timestamp = null
                            ),
                            createdAt = TestData.TIMESTAMP.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequest() {
        val id = UUID.randomUUID()
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = UUID.randomUUID(),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some asset send request will be fetched") {
            given(service.getAssetSendRequest(id))
                .willReturn(result)
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequest(id)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestResponse(
                            id = result.value.id,
                            projectId = result.value.projectId,
                            status = result.status,
                            chainId = result.value.chainId.value,
                            tokenAddress = result.value.tokenAddress?.rawValue,
                            assetType = AssetType.TOKEN,
                            amount = result.value.assetAmount.rawValue,
                            senderAddress = result.value.assetSenderAddress?.rawValue,
                            recipientAddress = result.value.assetRecipientAddress.rawValue,
                            arbitraryData = result.value.arbitraryData,
                            screenConfig = result.value.screenConfig,
                            redirectUrl = result.value.redirectUrl,
                            sendTx = TransactionResponse(
                                txHash = result.transactionData.txHash?.value,
                                from = result.transactionData.fromAddress?.rawValue,
                                to = result.transactionData.toAddress.rawValue,
                                data = result.transactionData.data?.value,
                                value = BigInteger.ZERO,
                                blockConfirmations = result.transactionData.blockConfirmations,
                                timestamp = TestData.TIMESTAMP.value
                            ),
                            createdAt = result.value.createdAt.value
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByProjectId() {
        val id = UUID.randomUUID()
        val projectId = UUID.randomUUID()
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = projectId,
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests will be fetched by project ID") {
            given(service.getAssetSendRequestsByProjectId(projectId))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsByProjectId(projectId)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsBySender() {
        val id = UUID.randomUUID()
        val sender = WalletAddress("b")
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = UUID.randomUUID(),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = sender,
                assetRecipientAddress = WalletAddress("c"),
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests will be fetched by sender") {
            given(service.getAssetSendRequestsBySender(sender))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsBySender(sender.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyFetchAssetSendRequestsByRecipient() {
        val id = UUID.randomUUID()
        val recipient = WalletAddress("c")
        val service = mock<AssetSendRequestService>()
        val txHash = TransactionHash("tx-hash")
        val result = WithTransactionData(
            value = AssetSendRequest(
                id = id,
                projectId = UUID.randomUUID(),
                chainId = ChainId(123L),
                redirectUrl = "redirect-url",
                tokenAddress = ContractAddress("a"),
                assetAmount = Balance(BigInteger.TEN),
                assetSenderAddress = WalletAddress("b"),
                assetRecipientAddress = recipient,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "before-action-message",
                    afterActionMessage = "after-action-message"
                ),
                txHash = txHash,
                createdAt = TestData.TIMESTAMP
            ),
            status = Status.SUCCESS,
            transactionData = TransactionData(
                txHash = txHash,
                fromAddress = WalletAddress("b"),
                toAddress = ContractAddress("a"),
                data = FunctionData("data"),
                value = Balance.ZERO,
                blockConfirmations = BigInteger.ONE,
                timestamp = TestData.TIMESTAMP
            )
        )

        suppose("some asset send requests will be fetched by recipient") {
            given(service.getAssetSendRequestsByRecipient(recipient))
                .willReturn(listOf(result))
        }

        val controller = AssetSendRequestController(service)

        verify("controller returns correct response") {
            val response = controller.getAssetSendRequestsByRecipient(recipient.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        AssetSendRequestsResponse(
                            listOf(
                                AssetSendRequestResponse(
                                    id = result.value.id,
                                    projectId = result.value.projectId,
                                    status = result.status,
                                    chainId = result.value.chainId.value,
                                    tokenAddress = result.value.tokenAddress?.rawValue,
                                    assetType = AssetType.TOKEN,
                                    amount = result.value.assetAmount.rawValue,
                                    senderAddress = result.value.assetSenderAddress?.rawValue,
                                    recipientAddress = result.value.assetRecipientAddress.rawValue,
                                    arbitraryData = result.value.arbitraryData,
                                    screenConfig = result.value.screenConfig,
                                    redirectUrl = result.value.redirectUrl,
                                    sendTx = TransactionResponse(
                                        txHash = result.transactionData.txHash?.value,
                                        from = result.transactionData.fromAddress?.rawValue,
                                        to = result.transactionData.toAddress.rawValue,
                                        data = result.transactionData.data?.value,
                                        value = BigInteger.ZERO,
                                        blockConfirmations = result.transactionData.blockConfirmations,
                                        timestamp = TestData.TIMESTAMP.value
                                    ),
                                    createdAt = result.value.createdAt.value
                                )
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAttachTransactionInfo() {
        val service = mock<AssetSendRequestService>()
        val controller = AssetSendRequestController(service)

        val id = UUID.randomUUID()
        val txHash = "tx-hash"
        val caller = "c"

        suppose("transaction info will be attached") {
            val request = AttachTransactionInfoRequest(txHash, caller)
            controller.attachTransactionInfo(id, request)
            JsonSchemaDocumentation.createSchema(request.javaClass)
        }

        verify("transaction info is correctly attached") {
            verifyMock(service)
                .attachTxInfo(id, TransactionHash(txHash), WalletAddress(caller))

            verifyNoMoreInteractions(service)
        }
    }
}
