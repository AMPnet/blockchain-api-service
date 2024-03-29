package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.config.binding.annotation.ApiKeyBinding
import com.ampnet.blockchainapiservice.config.validation.ValidEthAddress
import com.ampnet.blockchainapiservice.model.params.CreateAssetSendRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachTransactionInfoRequest
import com.ampnet.blockchainapiservice.model.request.CreateAssetSendRequest
import com.ampnet.blockchainapiservice.model.response.AssetSendRequestResponse
import com.ampnet.blockchainapiservice.model.response.AssetSendRequestsResponse
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.service.AssetSendRequestService
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.validation.annotation.Validated
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID
import javax.validation.Valid

@Validated
@RestController
class AssetSendRequestController(private val assetSendRequestService: AssetSendRequestService) {

    @PostMapping("/v1/send")
    fun createAssetSendRequest(
        @ApiKeyBinding project: Project,
        @Valid @RequestBody requestBody: CreateAssetSendRequest
    ): ResponseEntity<AssetSendRequestResponse> {
        val params = CreateAssetSendRequestParams(requestBody)
        val createdRequest = assetSendRequestService.createAssetSendRequest(params, project)
        return ResponseEntity.ok(AssetSendRequestResponse(createdRequest))
    }

    @GetMapping("/v1/send/{id}")
    fun getAssetSendRequest(
        @PathVariable("id") id: UUID
    ): ResponseEntity<AssetSendRequestResponse> {
        val sendRequest = assetSendRequestService.getAssetSendRequest(id)
        return ResponseEntity.ok(AssetSendRequestResponse(sendRequest))
    }

    @GetMapping("/v1/send/by-project/{projectId}")
    fun getAssetSendRequestsByProjectId(
        @PathVariable("projectId") projectId: UUID
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsByProjectId(projectId)
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @GetMapping("/v1/send/by-sender/{sender}")
    fun getAssetSendRequestsBySender(
        @ValidEthAddress @PathVariable("sender") sender: String
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsBySender(WalletAddress(sender))
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @GetMapping("/v1/send/by-recipient/{recipient}")
    fun getAssetSendRequestsByRecipient(
        @ValidEthAddress @PathVariable("recipient") recipient: String
    ): ResponseEntity<AssetSendRequestsResponse> {
        val sendRequests = assetSendRequestService.getAssetSendRequestsByRecipient(WalletAddress(recipient))
        return ResponseEntity.ok(AssetSendRequestsResponse(sendRequests.map { AssetSendRequestResponse(it) }))
    }

    @PutMapping("/v1/send/{id}")
    fun attachTransactionInfo(
        @PathVariable("id") id: UUID,
        @Valid @RequestBody requestBody: AttachTransactionInfoRequest
    ) {
        assetSendRequestService.attachTxInfo(
            id,
            TransactionHash(requestBody.txHash),
            WalletAddress(requestBody.callerAddress)
        )
    }
}
