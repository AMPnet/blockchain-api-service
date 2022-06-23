package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.blockchain.properties.RpcUrlSpec
import com.ampnet.blockchainapiservice.config.binding.annotation.RpcUrlBinding
import com.ampnet.blockchainapiservice.model.params.CreateErc20BalanceRequestParams
import com.ampnet.blockchainapiservice.model.request.AttachSignedMessageRequest
import com.ampnet.blockchainapiservice.model.request.CreateErc20BalanceRequest
import com.ampnet.blockchainapiservice.model.response.Erc20BalanceRequestResponse
import com.ampnet.blockchainapiservice.service.Erc20BalanceRequestService
import com.ampnet.blockchainapiservice.util.SignedMessage
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
class Erc20BalanceRequestController(private val erc20BalanceRequestService: Erc20BalanceRequestService) {

    @PostMapping("/v1/balance")
    fun createErc20BalanceRequest(
        @RequestBody requestBody: CreateErc20BalanceRequest
    ): ResponseEntity<Erc20BalanceRequestResponse> {
        val params = CreateErc20BalanceRequestParams(requestBody)
        val createdRequest = erc20BalanceRequestService.createErc20BalanceRequest(params)
        return ResponseEntity.ok(Erc20BalanceRequestResponse(createdRequest))
    }

    @GetMapping("/v1/balance/{id}")
    fun getErc20BalanceRequest(
        @PathVariable("id") id: UUID,
        @RpcUrlBinding rpcSpec: RpcUrlSpec
    ): ResponseEntity<Erc20BalanceRequestResponse> {
        val balanceRequest = erc20BalanceRequestService.getErc20BalanceRequest(id, rpcSpec)
        return ResponseEntity.ok(Erc20BalanceRequestResponse(balanceRequest))
    }

    @PutMapping("/v1/balance/{id}")
    fun attachSignedMessage(
        @PathVariable("id") id: UUID,
        @RequestBody requestBody: AttachSignedMessageRequest
    ) {
        erc20BalanceRequestService.attachWalletAddressAndSignedMessage(
            id = id,
            walletAddress = WalletAddress(requestBody.walletAddress),
            signedMessage = SignedMessage(requestBody.signedMessage)
        )
    }
}
