package com.ampnet.blockchainapiservice.model.request

import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.TestData
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants.FUNCTION_ARGUMENT_MAX_JSON_CHARS
import com.ampnet.blockchainapiservice.config.validation.ValidationConstants.REQUEST_BODY_MAX_JSON_CHARS
import com.ampnet.blockchainapiservice.model.ScreenConfig
import com.ampnet.blockchainapiservice.util.FunctionArgument
import com.ampnet.blockchainapiservice.util.JsonNodeConverter
import com.ampnet.blockchainapiservice.util.WalletAddress
import org.assertj.core.api.Assertions.assertThat
import org.jooq.JSON
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.web3j.abi.datatypes.generated.Uint256
import java.math.BigInteger
import javax.validation.Validation
import javax.validation.Validator
import javax.validation.ValidatorFactory

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CreateContractFunctionCallRequestTest : TestBase() {

    private lateinit var validatorFactory: ValidatorFactory
    private lateinit var validator: Validator

    @BeforeAll
    fun beforeAll() {
        validatorFactory = Validation.buildDefaultValidatorFactory()
        validator = validatorFactory.validator
    }

    @AfterAll
    fun afterAll() {
        validatorFactory.close()
    }

    @Test
    fun mustNotAllowTooLongStringForDeployedContractAlias() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("deployedContractAlias")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForContractAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = "invalid",
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("contractAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = WalletAddress("a").rawValue + "b",
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("contractAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = "",
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("contractAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = WalletAddress("a").rawValue,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForFunctionName() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("functionName")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidFunctionParams() {
        val requestWithTooLongListOfArguments = suppose("request with too long list of arguments is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = MutableList(ValidationConstants.REQUEST_BODY_MAX_ARGS_LENGTH + 1) {
                    FunctionArgument(Uint256.DEFAULT)
                },
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long list of arguments is marked as invalid") {
            val violations = validator.validate(requestWithTooLongListOfArguments).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 50")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("functionParams")
        }

        val tooLongValue = "a".repeat(FUNCTION_ARGUMENT_MAX_JSON_CHARS + 1)
        val requestWithTooLongArgumentJson = suppose("request with too long argument JSON is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = listOf(
                    FunctionArgument(
                        value = Uint256.DEFAULT,
                        rawJson = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}"))
                    )
                ),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long argument JSON is marked as invalid") {
            val violations = validator.validate(requestWithTooLongArgumentJson).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid JSON of at most 1000 characters")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("functionParams[0].rawJson")
        }

        val requestWithValidArguments = suppose("request with valid arguments is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = listOf(
                    FunctionArgument(
                        value = Uint256.DEFAULT,
                        rawJson = TestData.EMPTY_JSON_OBJECT
                    )
                ),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid arguments is marked as invalid") {
            val violations = validator.validate(requestWithValidArguments).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowNegativeOrTooBigValueForEthAmount() {
        val requestWithNegativeUint256 = suppose("request with negative uint256 is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.valueOf(-1L),
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with negative uint256 is marked as invalid") {
            val violations = validator.validate(requestWithNegativeUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("ethAmount")
        }

        val requestWithTooBigUint256 = suppose("request with too big uint256 is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.TWO.pow(256) + BigInteger.ONE,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too big uint256 is marked as invalid") {
            val violations = validator.validate(requestWithTooBigUint256).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be within range [0, 2^256]")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("ethAmount")
        }

        val requestWithValidUint256 = suppose("request with valid uint256 is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.TWO.pow(256),
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid uint256 is marked as valid") {
            val violations = validator.validate(requestWithValidUint256).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForRedirectUrl() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("redirectUrl")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                arbitraryData = null,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongJsonForArbitraryData() {
        val tooLongValue = "a".repeat(REQUEST_BODY_MAX_JSON_CHARS + 1)
        val requestWithTooLongJson = suppose("request with too long JSON is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = JsonNodeConverter().from(JSON.valueOf("{\"value\":\"$tooLongValue\"}")),
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with too long JSON is marked as invalid") {
            val violations = validator.validate(requestWithTooLongJson).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid JSON of at most 5000 characters")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("arbitraryData")
        }

        val requestWithValidLengthJson = suppose("request with valid length JSON is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = TestData.EMPTY_JSON_OBJECT,
                screenConfig = null,
                callerAddress = null
            )
        }

        verify("request with valid JSON string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthJson).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigBeforeActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1),
                    afterActionMessage = null
                ),
                callerAddress = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("screenConfig.beforeActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH),
                    afterActionMessage = null
                ),
                callerAddress = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowTooLongStringForScreenConfigAfterActionMessage() {
        val requestWithTooLongString = suppose("request with too long string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH + 1)
                ),
                callerAddress = null
            )
        }

        verify("request with too long string is marked as invalid") {
            val violations = validator.validate(requestWithTooLongString).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("size must be between 0 and 256")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("screenConfig.afterActionMessage")
        }

        val requestWithValidLengthString = suppose("request with valid length string is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = ScreenConfig(
                    beforeActionMessage = null,
                    afterActionMessage = "a".repeat(ValidationConstants.REQUEST_BODY_MAX_STRING_LENGTH)
                ),
                callerAddress = null
            )
        }

        verify("request with valid length string is marked as valid") {
            val violations = validator.validate(requestWithValidLengthString).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }

    @Test
    fun mustNotAllowInvalidEthAddressForCallerAddress() {
        val requestWithInvalidEthAddress = suppose("request with invalid eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = "invalid"
            )
        }

        verify("request with invalid eth address is marked as invalid") {
            val violations = validator.validate(requestWithInvalidEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithTooLongEthAddress = suppose("request with too long eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = WalletAddress("a").rawValue + "b"
            )
        }

        verify("request with too long eth address is marked as invalid") {
            val violations = validator.validate(requestWithTooLongEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithEmptyEthAddress = suppose("request with empty eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = ""
            )
        }

        verify("request with empty eth address is marked as invalid") {
            val violations = validator.validate(requestWithEmptyEthAddress).toList()

            assertThat(violations.size).withMessage()
                .isOne()
            assertThat(violations[0].message).withMessage()
                .isEqualTo("value must be a valid Ethereum address")
            assertThat(violations[0].propertyPath.toString()).withMessage()
                .isEqualTo("callerAddress")
        }

        val requestWithValidEthAddress = suppose("request with valid eth address is created") {
            CreateContractFunctionCallRequest(
                deployedContractId = null,
                deployedContractAlias = null,
                contractAddress = null,
                functionName = "",
                functionParams = emptyList(),
                ethAmount = BigInteger.ZERO,
                redirectUrl = null,
                arbitraryData = null,
                screenConfig = null,
                callerAddress = WalletAddress("a").rawValue
            )
        }

        verify("request with valid eth address is marked as valid") {
            val violations = validator.validate(requestWithValidEthAddress).toList()

            assertThat(violations).withMessage()
                .isEmpty()
        }
    }
}
