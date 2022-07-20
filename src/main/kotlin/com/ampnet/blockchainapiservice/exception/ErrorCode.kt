package com.ampnet.blockchainapiservice.exception

enum class ErrorCode {
    RESOURCE_NOT_FOUND,
    UNSUPPORTED_CHAIN_ID,
    TEMPORARY_BLOCKCHAIN_READ_ERROR,
    BLOCKCHAIN_READ_ERROR,
    TX_INFO_ALREADY_SET,
    SIGNED_MESSAGE_ALREADY_SET,
    BAD_AUTHENTICATION,
    API_KEY_ALREADY_EXISTS,
    NON_EXISTENT_API_KEY,
    MISSING_TOKEN_ADDRESS,
    TOKEN_ADDRESS_NOT_ALLOWED,
    DUPLICATE_ISSUER_CONTRACT_ADDRESS_FOR_CHAIN_ID,
    ALIAS_ALREADY_IN_USE
}
