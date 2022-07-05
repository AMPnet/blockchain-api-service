package com.ampnet.blockchainapiservice.service

import com.ampnet.blockchainapiservice.exception.CannotAttachTxInfoException
import com.ampnet.blockchainapiservice.model.params.CreateErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.params.StoreErc20SendRequestParams
import com.ampnet.blockchainapiservice.model.result.BlockchainTransactionInfo
import com.ampnet.blockchainapiservice.model.result.Erc20SendRequest
import com.ampnet.blockchainapiservice.model.result.Project
import com.ampnet.blockchainapiservice.repository.Erc20SendRequestRepository
import com.ampnet.blockchainapiservice.repository.ProjectRepository
import com.ampnet.blockchainapiservice.util.AbiType.AbiType
import com.ampnet.blockchainapiservice.util.Balance
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.FunctionData
import com.ampnet.blockchainapiservice.util.Status
import com.ampnet.blockchainapiservice.util.TransactionHash
import com.ampnet.blockchainapiservice.util.WalletAddress
import com.ampnet.blockchainapiservice.util.WithFunctionDataOrEthValue
import com.ampnet.blockchainapiservice.util.WithTransactionData
import mu.KLogging
import org.springframework.stereotype.Service
import java.util.UUID

@Service
@Suppress("TooManyFunctions")
class Erc20SendRequestServiceImpl(
    private val functionEncoderService: FunctionEncoderService,
    private val erc20SendRequestRepository: Erc20SendRequestRepository,
    private val erc20CommonService: Erc20CommonService,
    private val projectRepository: ProjectRepository
) : Erc20SendRequestService {

    companion object : KLogging()

    override fun createErc20SendRequest(
        params: CreateErc20SendRequestParams,
        project: Project
    ): WithFunctionDataOrEthValue<Erc20SendRequest> {
        logger.info { "Creating ERC20 send request, params: $params, project: $project" }

        val databaseParams = erc20CommonService.createDatabaseParams(StoreErc20SendRequestParams, params, project)
        val data = databaseParams.tokenAddress?.let {
            encodeFunctionData(params.tokenRecipientAddress, params.tokenAmount, databaseParams.id)
        }
        val ethValue = if (databaseParams.tokenAddress == null) databaseParams.tokenAmount else null
        val erc20SendRequest = erc20SendRequestRepository.store(databaseParams)

        return WithFunctionDataOrEthValue(erc20SendRequest, data, ethValue)
    }

    override fun getErc20SendRequest(id: UUID): WithTransactionData<Erc20SendRequest> {
        logger.debug { "Fetching ERC20 send request, id: $id" }

        val erc20SendRequest = erc20CommonService.fetchResource(
            erc20SendRequestRepository.getById(id),
            "ERC20 send request not found for ID: $id"
        )
        val project = projectRepository.getById(erc20SendRequest.projectId)!!

        return erc20SendRequest.appendTransactionData(project)
    }

    override fun getErc20SendRequestsByProjectId(projectId: UUID): List<WithTransactionData<Erc20SendRequest>> {
        logger.debug { "Fetching ERC20 send requests for projectId: $projectId" }
        val project = projectRepository.getById(projectId)!!
        return erc20SendRequestRepository.getAllByProjectId(projectId).map { it.appendTransactionData(project) }
    }

    override fun getErc20SendRequestsBySender(sender: WalletAddress): List<WithTransactionData<Erc20SendRequest>> {
        logger.debug { "Fetching ERC20 send requests for sender: $sender" }
        return erc20SendRequestRepository.getBySender(sender).map {
            val project = projectRepository.getById(it.projectId)!!
            it.appendTransactionData(project)
        }
    }

    override fun getErc20SendRequestsByRecipient(
        recipient: WalletAddress
    ): List<WithTransactionData<Erc20SendRequest>> {
        logger.debug { "Fetching ERC20 send requests for recipient: $recipient" }
        return erc20SendRequestRepository.getByRecipient(recipient).map {
            val project = projectRepository.getById(it.projectId)!!
            it.appendTransactionData(project)
        }
    }

    override fun attachTxInfo(id: UUID, txHash: TransactionHash, caller: WalletAddress) {
        logger.info { "Attach txInfo to ERC20 send request, id: $id, txHash: $txHash, caller: $caller" }

        val txInfoAttached = erc20SendRequestRepository.setTxInfo(id, txHash, caller)

        if (txInfoAttached.not()) {
            throw CannotAttachTxInfoException("Unable to attach transaction info to ERC20 send request with ID: $id")
        }
    }

    private fun encodeFunctionData(tokenRecipientAddress: WalletAddress, tokenAmount: Balance, id: UUID): FunctionData =
        functionEncoderService.encode(
            functionName = "transfer",
            arguments = listOf(
                FunctionArgument(abiType = AbiType.Address, value = tokenRecipientAddress),
                FunctionArgument(abiType = AbiType.Uint256, value = tokenAmount)
            ),
            abiOutputTypes = listOf(AbiType.Bool)
        )

    private fun Erc20SendRequest.appendTransactionData(project: Project): WithTransactionData<Erc20SendRequest> {
        val transactionInfo = erc20CommonService.fetchTransactionInfo(
            txHash = txHash,
            chainId = chainId,
            customRpcUrl = project.customRpcUrl
        )
        val data = tokenAddress?.let { encodeFunctionData(tokenRecipientAddress, tokenAmount, id) }
        val status = determineStatus(transactionInfo, data)

        return withTransactionData(
            status = status,
            data = data,
            value = if (tokenAddress == null) tokenAmount else null,
            transactionInfo = transactionInfo
        )
    }

    private fun Erc20SendRequest.determineStatus(
        transactionInfo: BlockchainTransactionInfo?,
        expectedData: FunctionData?
    ): Status =
        if (transactionInfo == null) { // implies that either txHash is null or transaction is not yet mined
            Status.PENDING
        } else if (isSuccess(transactionInfo, expectedData)) {
            Status.SUCCESS
        } else {
            Status.FAILED
        }

    private fun Erc20SendRequest.isSuccess(
        transactionInfo: BlockchainTransactionInfo,
        expectedData: FunctionData?
    ): Boolean =
        transactionInfo.success &&
            transactionInfo.hashMatches(txHash) &&
            transactionInfo.tokenAddressMatches(tokenAddress) &&
            transactionInfo.recipientAddressMatches(tokenAddress, tokenRecipientAddress) &&
            transactionInfo.senderAddressMatches(tokenSenderAddress) &&
            transactionInfo.dataMatches(expectedData) &&
            transactionInfo.valueMatches(if (tokenAddress == null) tokenAmount else null)

    private fun BlockchainTransactionInfo.tokenAddressMatches(tokenAddress: ContractAddress?): Boolean =
        (tokenAddress != null && to.toContractAddress() == tokenAddress) || tokenAddress == null

    private fun BlockchainTransactionInfo.recipientAddressMatches(
        tokenAddress: ContractAddress?,
        recipientAddress: WalletAddress
    ): Boolean = (tokenAddress == null && to.toWalletAddress() == recipientAddress) || tokenAddress != null

    private fun BlockchainTransactionInfo.hashMatches(expectedHash: TransactionHash?): Boolean =
        hash == expectedHash

    private fun BlockchainTransactionInfo.senderAddressMatches(senderAddress: WalletAddress?): Boolean =
        senderAddress == null || from == senderAddress

    private fun BlockchainTransactionInfo.dataMatches(expectedData: FunctionData?): Boolean =
        expectedData == null || data == expectedData

    private fun BlockchainTransactionInfo.valueMatches(expectedValue: Balance?): Boolean =
        expectedValue == null || value == expectedValue
}
