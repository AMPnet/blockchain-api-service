package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.exception.AliasAlreadyInUseException
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractDeploymentRequestTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.ContractMetadataTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ContractDeploymentRequestRecord
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDeploymentRequestFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.params.StoreContractDeploymentRequestParams
import com.ampnet.blockchainapiservice.model.result.ContractDeploymentRequest
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import mu.KLogging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Record
import org.jooq.impl.DSL
import org.jooq.impl.DSL.coalesce
import org.springframework.dao.DuplicateKeyException
import org.springframework.stereotype.Repository
import java.util.UUID

@Suppress("TooManyFunctions")
@Repository
class JooqContractDeploymentRequestRepository(
    private val dslContext: DSLContext
) : ContractDeploymentRequestRepository {

    companion object : KLogging() {
        private val CONTRACT_DEPLOYMENT_REQUEST_TABLE = ContractDeploymentRequestTable.CONTRACT_DEPLOYMENT_REQUEST
        private val CONTRACT_METADATA_TABLE = ContractMetadataTable.CONTRACT_METADATA
    }

    override fun store(params: StoreContractDeploymentRequestParams): ContractDeploymentRequest {
        logger.info { "Store contract deployment request, params: $params" }
        val record = ContractDeploymentRequestRecord(
            id = params.id,
            alias = params.alias,
            contractMetadataId = null,
            contractData = params.contractData,
            constructorParams = params.constructorParams,
            initialEthAmount = params.initialEthAmount,
            chainId = params.chainId,
            redirectUrl = params.redirectUrl,
            projectId = params.projectId,
            createdAt = params.createdAt,
            arbitraryData = params.arbitraryData,
            screenBeforeActionMessage = params.screenConfig.beforeActionMessage,
            screenAfterActionMessage = params.screenConfig.afterActionMessage,
            contractAddress = null,
            deployerAddress = params.deployerAddress,
            txHash = null,
            imported = params.imported,
            deleted = false
        )
        val selectContractMetadataId = DSL.select(CONTRACT_METADATA_TABLE.ID)
            .from(CONTRACT_METADATA_TABLE)
            .where(CONTRACT_METADATA_TABLE.CONTRACT_ID.eq(params.contractId))

        try {
            dslContext.insertInto(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
                .set(record)
                .set(CONTRACT_DEPLOYMENT_REQUEST_TABLE.CONTRACT_METADATA_ID, selectContractMetadataId)
                .execute()
        } catch (e: DuplicateKeyException) {
            throw AliasAlreadyInUseException(params.alias)
        }

        return getById(params.id)!!
    }

    override fun markAsDeleted(id: UUID): Boolean {
        logger.info { "Marking contract deployment request as deleted, id: $id" }
        return dslContext.update(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
            .set(CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED, true)
            .where(
                DSL.and(
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.ID.eq(id),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    override fun getById(id: UUID): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by id: $id" }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.ID.eq(id),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getByAliasAndProjectId(alias: String, projectId: UUID): ContractDeploymentRequest? {
        logger.debug { "Get contract deployment request by alias and project ID, alias: $alias, projectId: $projectId" }
        return dslContext.selectWithJoin()
            .where(
                DSL.and(
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.ALIAS.eq(alias),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.PROJECT_ID.eq(projectId),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false)
                )
            )
            .fetchOne { it.toModel() }
    }

    override fun getAllByProjectId(
        projectId: UUID,
        filters: ContractDeploymentRequestFilters
    ): List<ContractDeploymentRequest> {
        logger.debug { "Get contract deployment requests by projectId: $projectId, filters: $filters" }

        val conditions = listOfNotNull(
            CONTRACT_DEPLOYMENT_REQUEST_TABLE.PROJECT_ID.eq(projectId),
            CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false),
            filters.contractIds.orCondition(),
            filters.contractTags.orAndCondition { it.contractTagsAndCondition() },
            filters.contractImplements.orAndCondition { it.contractTraitsAndCondition() },
            filters.deployedOnly.takeIf { it }?.let { CONTRACT_DEPLOYMENT_REQUEST_TABLE.TX_HASH.isNotNull() }
        )

        return dslContext.selectWithJoin()
            .where(conditions)
            .orderBy(CONTRACT_DEPLOYMENT_REQUEST_TABLE.CREATED_AT.asc())
            .fetch { it.toModel() }
    }

    override fun setTxInfo(id: UUID, txHash: TransactionHash, deployer: WalletAddress): Boolean {
        logger.info { "Set txInfo for contract deployment request, id: $id, txHash: $txHash, deployer: $deployer" }
        return dslContext.update(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
            .set(CONTRACT_DEPLOYMENT_REQUEST_TABLE.TX_HASH, txHash)
            .set(
                CONTRACT_DEPLOYMENT_REQUEST_TABLE.DEPLOYER_ADDRESS,
                coalesce(CONTRACT_DEPLOYMENT_REQUEST_TABLE.DEPLOYER_ADDRESS, deployer)
            )
            .where(
                DSL.and(
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.ID.eq(id),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.TX_HASH.isNull(),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    override fun setContractAddress(id: UUID, contractAddress: ContractAddress): Boolean {
        logger.info {
            "Set contract address for contract deployment request, id: $id, contractAddress: $contractAddress"
        }
        return dslContext.update(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
            .set(CONTRACT_DEPLOYMENT_REQUEST_TABLE.CONTRACT_ADDRESS, contractAddress)
            .where(
                DSL.and(
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.ID.eq(id),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.CONTRACT_ADDRESS.isNull(),
                    CONTRACT_DEPLOYMENT_REQUEST_TABLE.DELETED.eq(false)
                )
            )
            .execute() > 0
    }

    private fun Record.toModel(): ContractDeploymentRequest {
        val requestRecord = this.into(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
        val metadataRecord = this.into(CONTRACT_METADATA_TABLE)

        return ContractDeploymentRequest(
            id = requestRecord.id!!,
            alias = requestRecord.alias!!,
            name = metadataRecord.name,
            description = metadataRecord.description,
            contractId = metadataRecord.contractId!!,
            contractData = requestRecord.contractData!!,
            constructorParams = requestRecord.constructorParams!!,
            contractTags = metadataRecord.contractTags!!.map { ContractTag(it!!) },
            contractImplements = metadataRecord.contractImplements!!.map { ContractTrait(it!!) },
            initialEthAmount = requestRecord.initialEthAmount!!,
            chainId = requestRecord.chainId!!,
            redirectUrl = requestRecord.redirectUrl!!,
            projectId = requestRecord.projectId!!,
            createdAt = requestRecord.createdAt!!,
            arbitraryData = requestRecord.arbitraryData,
            screenConfig = ScreenConfig(
                beforeActionMessage = requestRecord.screenBeforeActionMessage,
                afterActionMessage = requestRecord.screenAfterActionMessage
            ),
            contractAddress = requestRecord.contractAddress,
            deployerAddress = requestRecord.deployerAddress,
            txHash = requestRecord.txHash,
            imported = requestRecord.imported!!
        )
    }

    private fun DSLContext.selectWithJoin() = select()
        .from(CONTRACT_DEPLOYMENT_REQUEST_TABLE)
        .join(CONTRACT_METADATA_TABLE)
        .on(CONTRACT_DEPLOYMENT_REQUEST_TABLE.CONTRACT_METADATA_ID.eq(CONTRACT_METADATA_TABLE.ID))

    private fun OrList<ContractId>.orCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let { CONTRACT_METADATA_TABLE.CONTRACT_ID.`in`(it.list) }

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            CONTRACT_METADATA_TABLE.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<ContractTrait>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            CONTRACT_METADATA_TABLE.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
