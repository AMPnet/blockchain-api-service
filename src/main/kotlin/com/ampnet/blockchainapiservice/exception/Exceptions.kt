package com.ampnet.blockchainapiservice.exception

import com.ampnet.blockchainapiservice.util.ChainId
import com.ampnet.blockchainapiservice.util.ContractAddress
import com.ampnet.blockchainapiservice.util.ContractId
import org.springframework.http.HttpStatus
import java.util.UUID

abstract class ServiceException(
    val errorCode: ErrorCode,
    val httpStatus: HttpStatus,
    override val message: String,
    override val cause: Throwable? = null
) : RuntimeException(message) {
    companion object {
        private const val serialVersionUID: Long = 8974557457024980481L
    }
}

class ResourceNotFoundException(message: String) : ServiceException(
    errorCode = ErrorCode.RESOURCE_NOT_FOUND,
    httpStatus = HttpStatus.NOT_FOUND,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 8937915498141342807L
    }
}

class UnsupportedChainIdException(chainId: ChainId) : ServiceException(
    errorCode = ErrorCode.UNSUPPORTED_CHAIN_ID,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Blockchain id: ${chainId.value} not supported"
) {
    companion object {
        private const val serialVersionUID: Long = -8803854722161717146L
    }
}

class TemporaryBlockchainReadException(cause: Throwable? = null) : ServiceException(
    errorCode = ErrorCode.TEMPORARY_BLOCKCHAIN_READ_ERROR,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    message = "Error reading block data from blockchain RPC provider, please try again later",
    cause = cause
) {
    companion object {
        private const val serialVersionUID: Long = -4056606709117095199L
    }
}

class BlockchainReadException(message: String) : ServiceException(
    errorCode = ErrorCode.BLOCKCHAIN_READ_ERROR,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = -5979025245655611755L
    }
}

class CannotAttachTxInfoException(message: String) : ServiceException(
    errorCode = ErrorCode.TX_INFO_ALREADY_SET,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 2973294387463968605L
    }
}

class CannotAttachSignedMessageException(message: String) : ServiceException(
    errorCode = ErrorCode.SIGNED_MESSAGE_ALREADY_SET,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = 2487635142233013917L
    }
}

class BadAuthenticationException : ServiceException(
    errorCode = ErrorCode.BAD_AUTHENTICATION,
    httpStatus = HttpStatus.UNAUTHORIZED,
    message = "Provided authentication header has invalid format"
) {
    companion object {
        private const val serialVersionUID: Long = -787538305851627646L
    }
}

class ApiKeyAlreadyExistsException(projectId: UUID) : ServiceException(
    errorCode = ErrorCode.API_KEY_ALREADY_EXISTS,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "API key already exists for project with ID: $projectId"
) {
    companion object {
        private const val serialVersionUID: Long = 6676987534485377215L
    }
}

class NonExistentApiKeyException : ServiceException(
    errorCode = ErrorCode.NON_EXISTENT_API_KEY,
    httpStatus = HttpStatus.UNAUTHORIZED,
    message = "Non existent API key provided in request"
) {
    companion object {
        private const val serialVersionUID: Long = -176593491332037627L
    }
}

class MissingTokenAddressException : ServiceException(
    errorCode = ErrorCode.MISSING_TOKEN_ADDRESS,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Token address is missing from the request"
) {
    companion object {
        private const val serialVersionUID: Long = -8004673014736666252L
    }
}

class TokenAddressNotAllowedException : ServiceException(
    errorCode = ErrorCode.TOKEN_ADDRESS_NOT_ALLOWED,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Token address is not allowed for this request"
) {
    companion object {
        private const val serialVersionUID: Long = -2512631824095658324L
    }
}

class DuplicateIssuerContractAddressException(issuer: ContractAddress, chainId: ChainId) : ServiceException(
    errorCode = ErrorCode.DUPLICATE_ISSUER_CONTRACT_ADDRESS_FOR_CHAIN_ID,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Issuer address ${issuer.rawValue} is already assigned to a project with chain ID: ${chainId.value}"
) {
    companion object {
        private const val serialVersionUID: Long = -344230356993636309L
    }
}

class AliasAlreadyInUseException(alias: String) : ServiceException(
    errorCode = ErrorCode.ALIAS_ALREADY_IN_USE,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Alias '$alias' is already in use"
) {
    companion object {
        private const val serialVersionUID: Long = -3390991241297115163L
    }
}

class ContractNotYetDeployedException(id: UUID, alias: String) : ServiceException(
    errorCode = ErrorCode.CONTRACT_NOT_DEPLOYED,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Contract with ID: $id and alias: $alias is not yet deployed"
) {
    companion object {
        private const val serialVersionUID: Long = 4421635315803726161L
    }
}

class InvalidRequestBodyException(message: String) : ServiceException(
    errorCode = ErrorCode.INVALID_REQUEST_BODY,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = message
) {
    companion object {
        private const val serialVersionUID: Long = -3878710530557672092L
    }
}

class ContractNotFoundException(contractAddress: ContractAddress) : ServiceException(
    errorCode = ErrorCode.CONTRACT_NOT_FOUND,
    httpStatus = HttpStatus.NOT_FOUND,
    message = "Smart contract not found for contract address: ${contractAddress.rawValue}"
) {
    companion object {
        private const val serialVersionUID: Long = 4862558996776903135L
    }
}

class ContractDecoratorBinaryMismatchException(
    contractAddress: ContractAddress,
    contractId: ContractId
) : ServiceException(
    errorCode = ErrorCode.CONTRACT_BINARY_MISMATCH,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Smart contract at address ${contractAddress.rawValue} does not match definition of smart" +
        " contract with ID: ${contractId.value}"
) {
    companion object {
        private const val serialVersionUID: Long = 4385491497800278138L
    }
}

class CannotDecompileContractBinaryException : ServiceException(
    errorCode = ErrorCode.CANNOT_DECOMPILE_CONTRACT_BINARY,
    httpStatus = HttpStatus.BAD_REQUEST,
    message = "Contract binary of the requested smart contract cannot be successfully decompiled"
) {
    companion object {
        private const val serialVersionUID: Long = 2146904217800125145L
    }
}

class ContractDecompilationTemporarilyUnavailableException : ServiceException(
    errorCode = ErrorCode.CONTRACT_DECOMPILATION_TEMPORARILY_UNAVAILABLE,
    httpStatus = HttpStatus.SERVICE_UNAVAILABLE,
    message = "Contract binary decompilation is temporarily unavailable; try again at a later time"
) {
    companion object {
        private const val serialVersionUID: Long = 4124559311239162111L
    }
}
