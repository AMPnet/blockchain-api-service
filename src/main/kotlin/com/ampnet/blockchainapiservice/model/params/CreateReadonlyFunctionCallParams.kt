package com.ampnet.blockchainapiservice.model.params

import com.ampnet.blockchainapiservice.util.BlockNumber
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.WalletAddress

data class CreateReadonlyFunctionCallParams(
    val identifier: DeployedContractIdentifier,
    val blockNumber: BlockNumber?,
    val functionName: String,
    val functionParams: List<FunctionArgument>,
    val outputParameters: List<String>, // TODO use more specific type
    val callerAddress: WalletAddress
)
