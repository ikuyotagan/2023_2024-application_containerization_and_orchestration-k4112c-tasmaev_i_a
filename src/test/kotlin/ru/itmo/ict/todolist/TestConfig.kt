package ru.itmo.ict.todolist

import org.springframework.boot.fromApplication
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.boot.with
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Profile
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName

@TestConfiguration(proxyBeanMethods = false)
class TestConfig {
    @Bean
    @ServiceConnection
    @Profile("integration-test")
    fun postgresContainer(): PostgreSQLContainer<*> {
        return PostgreSQLContainer(DockerImageName
            .parse("postgres:14-alpine")
            .asCompatibleSubstituteFor("postgres"),
        )
    }
}

fun main() {
    fromApplication<TasksApiApplication>()
        .with(TestConfig::class)
        .run()
}
