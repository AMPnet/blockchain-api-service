package com.ampnet.blockchainapiservice.util

data class WithTransactionAndFunctionData<T>(
    val value: T,
    val functionData: FunctionData,
    val status: Status,
    val transactionData: TransactionData
)
