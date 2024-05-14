package ru.itmo.ict.todolist

import com.ninjasquad.springmockk.MockkBean
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.condition.EnabledIfSystemProperty
import org.springframework.boot.autoconfigure.EnableAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.ComponentScan.Filter
import org.springframework.context.annotation.FilterType
import java.time.Clock

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = [TestConfig::class],
)
@IntegrationTest
@EnableAutoConfiguration
abstract class AbstractIntegrationWebTest : AbstractIntegrationTest()

@SpringBootTest(
    classes = [TestConfig::class],
)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@IntegrationTest
@EnableAutoConfiguration
abstract class AbstractIntegrationTest : MockExternalApis()

@UnitTest
@SpringBootTest(
    classes = [TestConfig::class],
)
@EnableAutoConfiguration(
    exclude = [
        DataSourceAutoConfiguration::class,
        HibernateJpaAutoConfiguration::class,
        DataSourceTransactionManagerAutoConfiguration::class,
    ],
)
@ComponentScan(
    excludeFilters = [
        Filter(
            type = FilterType.ASSIGNABLE_TYPE,
            classes = [],
        ),
    ],
)
abstract class AbstractUnitSpringTest : MockExternalApis()

abstract class MockExternalApis {
    @MockkBean(name = "clock")
    lateinit var clock: Clock
}

@Tag("integration-test")
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "integration-test")
annotation class IntegrationTest

@Tag("unit-test")
@EnabledIfSystemProperty(named = "spring.profiles.active", matches = "unit-test")
annotation class UnitTest
