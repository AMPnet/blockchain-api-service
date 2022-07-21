package com.ampnet.blockchainapiservice.controller

import com.ampnet.blockchainapiservice.ControllerTestBase
import com.ampnet.blockchainapiservice.blockchain.ExampleContract
import com.ampnet.blockchainapiservice.exception.ErrorCode
import com.ampnet.blockchainapiservice.model.filters.ContractDecoratorFilters
import com.ampnet.blockchainapiservice.model.filters.OrList
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorResponse
import com.ampnet.blockchainapiservice.model.response.ContractDecoratorsResponse
import com.ampnet.blockchainapiservice.model.result.ContractConstructor
import com.ampnet.blockchainapiservice.model.result.ContractDecorator
import com.ampnet.blockchainapiservice.model.result.ContractFunction
import com.ampnet.blockchainapiservice.model.result.ContractParameter
import com.ampnet.blockchainapiservice.repository.ContractDecoratorRepository
import com.ampnet.blockchainapiservice.util.ContractBinaryData
import com.ampnet.blockchainapiservice.util.ContractId
import com.ampnet.blockchainapiservice.util.ContractTag
import com.ampnet.blockchainapiservice.util.ContractTrait
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders
import org.springframework.test.web.servlet.result.MockMvcResultMatchers
import java.util.UUID

class ContractDecoratorControllerApiTest : ControllerTestBase() {

    companion object {
        private val CONTRACT_DECORATOR = ContractDecorator(
            id = ContractId("examples.exampleContract"),
            binary = ContractBinaryData(ExampleContract.BINARY),
            tags = listOf(ContractTag("example"), ContractTag("simple")),
            implements = listOf(ContractTrait("traits.example"), ContractTrait("traits.exampleOwnable")),
            constructors = listOf(
                ContractConstructor(
                    inputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "owner",
                            solidityType = "address",
                            recommendedTypes = listOf()
                        )
                    ),
                    description = "Main constructor",
                    payable = true
                )
            ),
            functions = listOf(
                ContractFunction(
                    name = "Get contract owner",
                    description = "Fetches contract owner",
                    solidityName = "getOWner",
                    inputs = listOf(),
                    outputs = listOf(
                        ContractParameter(
                            name = "Owner address",
                            description = "Contract owner address",
                            solidityName = "",
                            solidityType = "address",
                            recommendedTypes = listOf()
                        )
                    ),
                    emittableEvents = emptyList(),
                    readOnly = true
                )
            ),
            events = listOf()
        )
    }

    @Autowired
    private lateinit var contractDecoratorRepository: ContractDecoratorRepository

    @BeforeEach
    fun beforeEach() {
        contractDecoratorRepository.getAll(ContractDecoratorFilters(OrList(), OrList())).forEach {
            contractDecoratorRepository.delete(it.id)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecorator() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorator is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${CONTRACT_DECORATOR.id.value}")
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDecoratorResponse(
                        id = CONTRACT_DECORATOR.id.value,
                        binary = CONTRACT_DECORATOR.binary.value,
                        tags = CONTRACT_DECORATOR.tags.map { it.value },
                        implements = CONTRACT_DECORATOR.implements.map { it.value },
                        constructors = CONTRACT_DECORATOR.constructors,
                        functions = CONTRACT_DECORATOR.functions,
                        events = CONTRACT_DECORATOR.events
                    )
                )
        }
    }

    @Test
    fun mustReturn404NotFoundForNonExistentContractDecorator() {
        verify("404 is returned for non-existent contract deployment request") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get("/v1/deployable-contracts/${UUID.randomUUID()}")
            )
                .andExpect(MockMvcResultMatchers.status().isNotFound)
                .andReturn()

            verifyResponseErrorCode(response, ErrorCode.RESOURCE_NOT_FOUND)
        }
    }

    @Test
    fun mustCorrectlyFetchContractDecoratorsWithFilters() {
        suppose("some contract decorator exists in the database") {
            contractDecoratorRepository.store(CONTRACT_DECORATOR)
        }

        val response = suppose("request to fetch contract decorators is made") {
            val response = mockMvc.perform(
                MockMvcRequestBuilders.get(
                    "/v1/deployable-contracts/?tags=example AND simple,other" +
                        "&implements=traits/example AND traits/exampleOwnable,traits/other"
                )
            )
                .andExpect(MockMvcResultMatchers.status().isOk)
                .andReturn()

            objectMapper.readValue(response.response.contentAsString, ContractDecoratorsResponse::class.java)
        }

        verify("correct response is returned") {
            assertThat(response).withMessage()
                .isEqualTo(
                    ContractDecoratorsResponse(
                        listOf(
                            ContractDecoratorResponse(
                                id = CONTRACT_DECORATOR.id.value,
                                binary = CONTRACT_DECORATOR.binary.value,
                                tags = CONTRACT_DECORATOR.tags.map { it.value },
                                implements = CONTRACT_DECORATOR.implements.map { it.value },
                                constructors = CONTRACT_DECORATOR.constructors,
                                functions = CONTRACT_DECORATOR.functions,
                                events = CONTRACT_DECORATOR.events
                            )
                        )
                    )
                )
        }
    }
}