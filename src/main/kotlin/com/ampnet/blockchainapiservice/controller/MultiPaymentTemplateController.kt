package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.UserIdentifierBinding
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.request.CreateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.request.MultiPaymentTemplateItemRequest
import com.ampnet.blockchainapiservice.model.request.UpdateMultiPaymentTemplateRequest
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplateWithItemsResponse
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplateWithoutItemsResponse
import com.ampnet.blockchainapiservice.model.response.MultiPaymentTemplatesResponse
import com.ampnet.blockchainapiservice.model.result.UserIdentifier
import com.ampnet.blockchainapiservice.service.MultiPaymentTemplateService
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class MultiPaymentTemplateController(private val multiPaymentTemplateService: MultiPaymentTemplateService) {

    @PostMapping("/v1/multi-payment-template")
    fun createMultiPaymentTemplate(
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: CreateMultiPaymentTemplateRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.createMultiPaymentTemplate(
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/multi-payment-template/{id}")
    fun updateMultiPaymentTemplate(
        @PathVariable("id") id: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: UpdateMultiPaymentTemplateRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.updateMultiPaymentTemplate(
                    templateId = id,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/multi-payment-template/{id}")
    fun deleteMultiPaymentTemplate(
        @PathVariable("id") id: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ) {
        multiPaymentTemplateService.deleteMultiPaymentTemplateById(
            templateId = id,
            userIdentifier = userIdentifier
        )
    }

    @GetMapping("/v1/multi-payment-template/{id}")
    fun getMultiPaymentTemplateById(
        @PathVariable("id") id: UUID
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.getMultiPaymentTemplateById(id)
            )
        )
    }

    @GetMapping("/v1/multi-payment-template/by-wallet-address/{walletAddress}")
    fun getAllMultiPaymentTemplatesByWalletAddress(
        @ValidEthAddress @PathVariable("walletAddress") walletAddress: String
    ): ResponseEntity<MultiPaymentTemplatesResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplatesResponse(
                multiPaymentTemplateService.getAllMultiPaymentTemplatesByWalletAddress(WalletAddress(walletAddress))
                    .map { MultiPaymentTemplateWithoutItemsResponse(it) }
            )
        )
    }

    @PostMapping("/v1/multi-payment-template/{templateId}/items")
    fun addItemToMultiPaymentTemplate(
        @PathVariable("templateId") templateId: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: MultiPaymentTemplateItemRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.addItemToMultiPaymentTemplate(
                    templateId = templateId,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @PatchMapping("/v1/multi-payment-template/{templateId}/items/{itemId}")
    fun updateMultiPaymentTemplateItem(
        @PathVariable("templateId") templateId: UUID,
        @PathVariable("itemId") itemId: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier,
        @Valid @RequestBody requestBody: MultiPaymentTemplateItemRequest
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.updateMultiPaymentTemplateItem(
                    templateId = templateId,
                    itemId = itemId,
                    request = requestBody,
                    userIdentifier = userIdentifier
                )
            )
        )
    }

    @DeleteMapping("/v1/multi-payment-template/{templateId}/items/{itemId}")
    fun deleteMultiPaymentTemplateItem(
        @PathVariable("templateId") templateId: UUID,
        @PathVariable("itemId") itemId: UUID,
        @UserIdentifierBinding userIdentifier: UserIdentifier
    ): ResponseEntity<MultiPaymentTemplateWithItemsResponse> {
        return ResponseEntity.ok(
            MultiPaymentTemplateWithItemsResponse(
                multiPaymentTemplateService.deleteMultiPaymentTemplateItem(
                    templateId = templateId,
                    itemId = itemId,
                    userIdentifier = userIdentifier
                )
            )
        )
    }
}
