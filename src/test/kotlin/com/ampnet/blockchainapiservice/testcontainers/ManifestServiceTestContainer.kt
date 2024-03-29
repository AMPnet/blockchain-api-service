package com.ampnet.blockchainapiservice.testcontainers

import org.testcontainers.containers.GenericContainer
import org.testcontainers.containers.wait.strategy.LogMessageWaitStrategy
import java.time.Duration
import java.time.temporal.ChronoUnit

class ManifestServiceTestContainer : GenericContainer<ManifestServiceTestContainer>(
    "ampnet/contracts-manifest-service:0.2.0"
) {

    @Suppress("unused")
    companion object {
        private const val SERVICE_PORT = 42070
    }

    init {
        waitStrategy = LogMessageWaitStrategy()
            .withRegEx("Example app listening at .*")
            .withTimes(1)
            .withStartupTimeout(Duration.of(60, ChronoUnit.SECONDS))

        addExposedPort(SERVICE_PORT)
        start()

        val mappedPort = getMappedPort(SERVICE_PORT).toString()

        System.setProperty("MANIFEST_SERVICE_PORT", mappedPort)
    }
}
