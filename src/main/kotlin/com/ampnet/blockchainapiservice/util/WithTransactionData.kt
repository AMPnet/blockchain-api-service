package com.ampnet.blockchainapiservice.util

import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import java.math.BigInteger

data class WithTransactionData<T>(val value: T, val status: Status, val transactionData: TransactionData)

data class TransactionData(
    val txHash: TransactionHash?,
    val fromAddress: WalletAddress?,
    val toAddress: EthereumAddress,
    val data: FunctionData?,
    val value: Balance,
    val blockConfirmations: BigInteger?,
    val timestamp: UtcDateTime?
) {
    constructor(
        txHash: TransactionHash?,
        transactionInfo: BlockchainTransactionInfo?,
        fromAddress: WalletAddress?,
        toAddress: EthereumAddress,
        data: FunctionData?,
        value: Balance?
    ) : this(
        txHash = txHash,
        fromAddress = transactionInfo?.from ?: fromAddress,
        toAddress = transactionInfo?.to ?: toAddress,
        data = transactionInfo?.data ?: data,
        value = transactionInfo?.value ?: value ?: Balance.ZERO,
        blockConfirmations = transactionInfo?.blockConfirmations,
        timestamp = transactionInfo?.timestamp
    )
}
