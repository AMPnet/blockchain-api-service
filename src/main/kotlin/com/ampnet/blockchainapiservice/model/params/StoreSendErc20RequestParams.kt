package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.model.SendScreenConfig
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.fasterxml.jackson.databind.JsonNode
import java.util.UUID

data class StoreSendErc20RequestParams(
    val id: UUID,
    val chainId: ChainId,
    val redirectUrl: String,
    val tokenAddress: ContractAddress,
    val amount: Balance,
    val fromAddress: WalletAddress?,
    val toAddress: WalletAddress,
    val arbitraryData: JsonNode,
    val screenConfig: SendScreenConfig
)
