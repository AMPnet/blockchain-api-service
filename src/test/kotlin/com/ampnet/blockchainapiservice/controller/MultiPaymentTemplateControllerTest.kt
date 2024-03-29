package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import com.ampnet.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplateWithItemsResponse
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplateWithoutItemsResponse
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplatesResponse
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplate
import com.ampnet.blockchainapiservice.model.result.MultiPaymentTemplateItem
import com.ampnet.blockchainapiservice.model.result.UserWalletAddressIdentifier
import com.ampnet.blockchainapiservice.model.result.WithItems
import com.ampnet.blockchainapiservice.service.MultiPaymentTemplateService
import com.ampnet.blockchainapiservice.util.AssetType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.verifyNoMoreInteractions
import org.springframework.http.ResponseEntity
import java.math.BigInteger
import java.util.UUID
import org.mockito.kotlin.verify as verifyMock

class MultiPaymentTemplateControllerTest : TestBase() {

    companion object {
        private val USER_IDENTIFIER = UserWalletAddressIdentifier(
            id = UUID.randomUUID(),
            walletAddress = WalletAddress("cafebabe")
        )
        private val TEMPLATE_ID = UUID.randomUUID()
        private val ITEM = MultiPaymentTemplateItem(
            id = UUID.randomUUID(),
            templateId = TEMPLATE_ID,
            walletAddress = WalletAddress("a"),
            itemName = "itemName",
            assetAmount = Balance(BigInteger.TEN),
            createdAt = TestData.TIMESTAMP
        )
        private val TEMPLATE = MultiPaymentTemplate(
            id = TEMPLATE_ID,
            items = WithItems(listOf(ITEM)),
            templateName = "templateName",
            tokenAddress = ContractAddress("b"),
            chainId = ChainId(1337L),
            userId = USER_IDENTIFIER.id,
            createdAt = TestData.TIMESTAMP,
            updatedAt = null
        )
    }

    @Test
    fun mustCorrectlyCreateMultiPaymentTemplate() {
        val request = CreateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value,
            items = listOf(
                MultiPaymentTemplateItemRequest(
                    walletAddress = ITEM.walletAddress.rawValue,
                    itemName = ITEM.itemName,
                    amount = ITEM.assetAmount.rawValue
                )
            )
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be created") {
            given(service.createMultiPaymentTemplate(request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.createMultiPaymentTemplate(USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplate() {
        val request = UpdateMultiPaymentTemplateRequest(
            templateName = TEMPLATE.templateName,
            assetType = AssetType.TOKEN,
            tokenAddress = TEMPLATE.tokenAddress?.rawValue,
            chainId = TEMPLATE.chainId.value
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be updated") {
            given(service.updateMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplate() {
        val service = mock<MultiPaymentTemplateService>()
        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            controller.deleteMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER)

            verifyMock(service)
                .deleteMultiPaymentTemplateById(TEMPLATE_ID, USER_IDENTIFIER)
            verifyNoMoreInteractions(service)
        }
    }

    @Test
    fun mustCorrectlyFetchMultiPaymentTemplateById() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            given(service.getMultiPaymentTemplateById(TEMPLATE_ID))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getMultiPaymentTemplateById(TEMPLATE_ID)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyFetchAllMultiPaymentTemplatesByWalletAddress() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template will be fetched") {
            given(service.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress))
                .willReturn(listOf(TEMPLATE.withoutItems()))
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.getAllMultiPaymentTemplatesByWalletAddress(USER_IDENTIFIER.walletAddress.rawValue)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(
                    ResponseEntity.ok(
                        MultiPaymentTemplatesResponse(
                            listOf(
                                MultiPaymentTemplateWithoutItemsResponse(TEMPLATE.withoutItems())
                            )
                        )
                    )
                )
        }
    }

    @Test
    fun mustCorrectlyAddItemToMultiPaymentTemplate() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be created") {
            given(service.addItemToMultiPaymentTemplate(TEMPLATE_ID, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.addItemToMultiPaymentTemplate(TEMPLATE_ID, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyUpdateMultiPaymentTemplateItem() {
        val request = MultiPaymentTemplateItemRequest(
            walletAddress = ITEM.walletAddress.rawValue,
            itemName = ITEM.itemName,
            amount = ITEM.assetAmount.rawValue
        )

        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be updated") {
            given(service.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, request, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.updateMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER, request)

            JsonSchemaDocumentation.createSchema(request.javaClass)
            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }

    @Test
    fun mustCorrectlyDeleteMultiPaymentTemplateItem() {
        val service = mock<MultiPaymentTemplateService>()

        suppose("multi-payment template item will be deleted") {
            given(service.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER))
                .willReturn(TEMPLATE)
        }

        val controller = MultiPaymentTemplateController(service)

        verify("controller returns correct response") {
            val response = controller.deleteMultiPaymentTemplateItem(TEMPLATE_ID, ITEM.id, USER_IDENTIFIER)

            JsonSchemaDocumentation.createSchema(response.body!!.javaClass)

            assertThat(response).withMessage()
                .isEqualTo(ResponseEntity.ok(MultiPaymentTemplateWithItemsResponse(TEMPLATE)))
        }
    }
}
