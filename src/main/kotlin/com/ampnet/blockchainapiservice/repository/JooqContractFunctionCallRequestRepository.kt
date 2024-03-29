package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractFunctionCallRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.interfaces.IContractFunctionCallRequestRecord
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractFunctionCallRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.ContractFunctionCallRequestFilters
import com.ampnet.blockchainapiservice.model.params.StoreContractFunctionCallRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractFunctionCallRequest
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
class JooqContractFunctionCallRequestRepository(
    private val dslContext: DSLContext
) : ContractFunctionCallRequestRepository {

    companion object : KLogging() {
        private val TABLE = ContractFunctionCallRequestTable.CONTRACT_FUNCTION_CALL_REQUEST
    }

    override fun store(params: StoreContractFunctionCallRequestParams): ContractFunctionCallRequest {
        logger.info { "Store contract function call request, params: $params" }
        val record = ContractFunctionCallRequestRecord(
            id = params.id,
            deployedContractId = params.deployedContractId,
            contractAddress = params.contractAddress,
            functionName = params.functionName,
            functionParams = params.functionParams,
            ethAmount = params.ethAmount,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            projectId = params.projectId,
            createdAt = params.createdAt,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            callerAddress = params.callerAddress,
            txHash = null
        )
        dslContext.executeInsert(record)
        return record.toModel()
    }

    override fun getById(id: UUID): ContractFunctionCallRequest? {
        logger.debug { "Get contract function call request by id: $id" }
        return dslContext.selectFrom(TABLE)
            .where(TABLE.ID.eq(id))
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: UUID,
        filters: ContractFunctionCallRequestFilters
    ): List<ContractFunctionCallRequest> {
        logger.debug { "Get contract function call requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            TABLE.PROJECT_ID.eq(projectId),
            filters.deployedContractId?.let { TABLE.DEPLOYED_CONTRACT_ID.eq(it) },
            filters.contractAddress?.let { TABLE.CONTRACT_ADDRESS.eq(it) },
        )

        return dslContext.selectFrom(TABLE)
            .where(conditions)
            .orderBy(TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress): Boolean {
        logger.info { "Set txInfo for contract function call request, id: $id, txHash: $txHash, caller: $caller" }
        return dslContext.update(TABLE)
            .set(TABLE.TX_HASH, txHash)
            .set(TABLE.CALLER_ADDRESS, coalesce(TABLE.CALLER_ADDRESS, caller))
            .where(
                DSL.and(
                    TABLE.ID.eq(id),
                    TABLE.TX_HASH.isNull()
                )
            )
            .execute() > 0
    }

    private fun IContractFunctionCallRequestRecord.toModel() =
        ContractFunctionCallRequest(
            id = id!!,
            deployedContractId = deployedContractId,
            contractAddress = contractAddress!!,
            functionName = functionName!!,
            functionParams = functionParams!!,
            ethAmount = ethAmount!!,
            chainId = chainId!!,
            redirectUrl = redirectUrl!!,
            projectId = projectId!!,
            createdAt = createdAt!!,
            arbitraryData = arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = screenBeforeActionMessage,
                afterActionMessage = screenAfterActionMessage
            ),
            callerAddress = callerAddress,
            txHash = txHash,
        )
}
