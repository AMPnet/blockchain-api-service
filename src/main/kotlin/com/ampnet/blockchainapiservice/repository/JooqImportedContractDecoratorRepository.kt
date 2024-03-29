package com.ampnet.blockchainapiservice.repository

import com.ampnet.blockchainapiservice.generated.jooq.tables.ImportedContractDecoratorTable
import com.ampnet.blockchainapiservice.generated.jooq.tables.records.ImportedContractDecoratorRecord
import com.ampnet.blockchainapiservice.model.filters.AndList
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.json.ArtifactJson
import com.ampnet.blockchainapiservice.model.json.ManifestJson
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import com.ampnet.blockchainapiservice.util.UtcDateTime
import mu.KLogging
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
@Suppress("TooManyFunctions")
class JooqImportedContractDecoratorRepository(
    private val dslContext: DSLContext
) : ImportedContractDecoratorRepository {

    companion object : KLogging() {
        private val TABLE = ImportedContractDecoratorTable.IMPORTED_CONTRACT_DECORATOR
    }

    override fun store(
        id: UUID,
        projectId: UUID,
        contractId: ContractId,
        manifestJson: ManifestJson,
        artifactJson: ArtifactJson,
        infoMarkdown: String,
        importedAt: UtcDateTime
    ): ContractDecorator {
        logger.info { "Store imported contract decorator, id: $id, projectId: $projectId, contractId: $contractId" }
        val record = ImportedContractDecoratorRecord(
            id = id,
            projectId = projectId,
            contractId = contractId,
            manifestJson = manifestJson,
            artifactJson = artifactJson,
            infoMarkdown = infoMarkdown,
            contractTags = manifestJson.tags.toTypedArray(),
            contractImplements = manifestJson.implements.toTypedArray(),
            importedAt = importedAt
        )
        dslContext.executeInsert(record)
        return ContractDecorator(contractId, artifactJson, manifestJson)
    }

    override fun getByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ContractDecorator? {
        logger.debug { "Get imported contract decorator by contract id: $contractId" }
        return dslContext.selectFrom(TABLE)
            .where(
                DSL.and(
                    TABLE.CONTRACT_ID.eq(contractId),
                    TABLE.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.let {
                ContractDecorator(
                    id = it.contractId!!,
                    artifact = it.artifactJson!!,
                    manifest = it.manifestJson!!
                )
            }
    }

    override fun getManifestJsonByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ManifestJson? {
        logger.debug { "Get imported manifest.json by contract id: $contractId, project id: $projectId" }
        return dslContext.select(TABLE.MANIFEST_JSON)
            .from(TABLE)
            .where(
                DSL.and(
                    TABLE.CONTRACT_ID.eq(contractId),
                    TABLE.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getArtifactJsonByContractIdAndProjectId(contractId: ContractId, projectId: UUID): ArtifactJson? {
        logger.debug { "Get imported artifact.json by contract id: $contractId, project id: $projectId" }
        return dslContext.select(TABLE.ARTIFACT_JSON)
            .from(TABLE)
            .where(
                DSL.and(
                    TABLE.CONTRACT_ID.eq(contractId),
                    TABLE.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getInfoMarkdownByContractIdAndProjectId(contractId: ContractId, projectId: UUID): String? {
        logger.debug { "Get imported info.md by contract id: $contractId, project id: $projectId" }
        return dslContext.select(TABLE.INFO_MARKDOWN)
            .from(TABLE)
            .where(
                DSL.and(
                    TABLE.CONTRACT_ID.eq(contractId),
                    TABLE.PROJECT_ID.eq(projectId)
                )
            )
            .fetchOne()
            ?.value1()
    }

    override fun getAll(projectId: UUID, filters: ContractDecoratorFilters): List<ContractDecorator> {
        logger.debug { "Get imported contract decorators by projectId: $projectId, filters: $filters" }
        return dslContext.selectFrom(TABLE)
            .where(createConditions(projectId, filters))
            .orderBy(TABLE.IMPORTED_AT.asc())
            .fetch {
                ContractDecorator(
                    id = it.contractId!!,
                    artifact = it.artifactJson!!,
                    manifest = it.manifestJson!!
                )
            }
    }

    override fun getAllManifestJsonFiles(projectId: UUID, filters: ContractDecoratorFilters): List<ManifestJson> {
        logger.debug { "Get imported manifest.json files by projectId: $projectId, filters: $filters" }
        return dslContext.select(TABLE.MANIFEST_JSON)
            .from(TABLE)
            .where(createConditions(projectId, filters))
            .orderBy(TABLE.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    override fun getAllArtifactJsonFiles(projectId: UUID, filters: ContractDecoratorFilters): List<ArtifactJson> {
        logger.debug { "Get imported artifact.json files by projectId: $projectId, filters: $filters" }
        return dslContext.select(TABLE.ARTIFACT_JSON)
            .from(TABLE)
            .where(createConditions(projectId, filters))
            .orderBy(TABLE.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    override fun getAllInfoMarkdownFiles(projectId: UUID, filters: ContractDecoratorFilters): List<String> {
        logger.debug { "Get imported info.md files by projectId: $projectId, filters: $filters" }
        return dslContext.select(TABLE.INFO_MARKDOWN)
            .from(TABLE)
            .where(createConditions(projectId, filters))
            .orderBy(TABLE.IMPORTED_AT.asc())
            .fetch { it.value1() }
    }

    private fun createConditions(projectId: UUID, filters: ContractDecoratorFilters) =
        listOfNotNull(
            TABLE.PROJECT_ID.eq(projectId),
            filters.contractTags.orAndCondition { it.contractTagsAndCondition() },
            filters.contractImplements.orAndCondition { it.contractTraitsAndCondition() }
        )

    private fun AndList<ContractTag>.contractTagsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            TABLE.CONTRACT_TAGS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun AndList<ContractTrait>.contractTraitsAndCondition(): Condition? =
        takeIf { list.isNotEmpty() }?.let {
            TABLE.CONTRACT_IMPLEMENTS.contains(
                it.list.map { v -> v.value }.toTypedArray()
            )
        }

    private fun <T> OrList<AndList<T>>.orAndCondition(innerConditionMapping: (AndList<T>) -> Condition?): Condition? =
        list.mapNotNull(innerConditionMapping).takeIf { it.isNotEmpty() }?.let { DSL.or(it) }
}
