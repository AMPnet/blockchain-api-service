package com.ampnet.blockchainapiservice.exception

import com.ampnet.blockchainapiservice.JsonSchemaDocumentation
import com.ampnet.blockchainapiservice.TestBase
import com.ampnet.blockchainapiservice.util.annotation.Description
import org.junit.jupiter.api.Test
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class ErrorCodeTest : TestBase() {

    @Test
    fun mustGenerateErrorCodeDocumentation() {
        val descriptions = ErrorCode.values().joinToString(separator = "\n") {
            val description = ErrorCode::class.java.getField(it.name).getAnnotation(Description::class.java).value
            "|`${it.name}`\n|$description\n"
        }
        Files.createDirectories(Paths.get("build/generated-snippets"))
        Files.writeString(
            Paths.get("build/generated-snippets/${ErrorCode::class.java.typeName}.adoc"),
            ".Error Codes\n" +
                "[%collapsible]\n" +
                "====\n" +
                "[cols=\"1,1\"]\n" +
                "|===\n" +
                "|Error Code |Description\n\n" +
                descriptions +
                "\n|===\n" +
                "====\n",
            StandardOpenOption.CREATE
        )

        JsonSchemaDocumentation.createSchema(ErrorResponse::class.java)
    }
}
