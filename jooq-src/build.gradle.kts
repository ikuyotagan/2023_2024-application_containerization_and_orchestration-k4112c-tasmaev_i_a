import nu.studer.gradle.jooq.JooqEdition
import nu.studer.gradle.jooq.JooqGenerate
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jooq.meta.jaxb.ForcedType
import org.jooq.meta.jaxb.Logging
import org.liquibase.gradle.LiquibaseTask
import org.testcontainers.containers.PostgreSQLContainer

plugins {
    id("java-library")
    id("maven-publish")
    kotlin("jvm") version "1.9.22"

    id("nu.studer.jooq") version "9.0"
    id("org.liquibase.gradle") version "2.2.1"
    id("com.dorongold.task-tree") version "2.1.1"
}

val javaPackage = "ru.itmo.ict.todolist.generated.jooq"
group = javaPackage
version = "1.0.0"

repositories {
    mavenCentral()
    maven(url = uri("https://plugins.gradle.org/m2/"))
}

java {
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }
}

tasks.jar {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE

    from({
        configurations.runtimeClasspath.get().filter { it.name.endsWith("jar") }.map { zipTree(it) }
    })
}

// Здесь объявляются зависимости build-скрипта. Они не попадут в код приложения.
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("org.testcontainers:postgresql:1.19.3")
        classpath("org.liquibase:liquibase-core:4.25.1")
        classpath("net.java.dev.jna:jna:5.14.0")
        classpath("org.springframework.boot:spring-boot-testcontainers:3.2.1")
    }
}

val springBootVersion = "2.7.17"
val jakartaAnnotationVersion = "1.3.5"
val reactorVersion = "3.4.34"
val reactorNettyVersion = "1.0.39"
val jacksonVersion = "2.14.3"
val jacksonDatabindVersion = "2.14.3"
val swaggerAnnotationsVersion = "1.6.11"
val feignVersion = "10.12"
val feignFormVersion = "3.8.0"
val scribejavaVersion = "8.0.0"
val jooqVersion = "3.19.1"

dependencies {
    runtimeOnly("org.postgresql:postgresql:42.7.1")
    api("org.jooq:jooq-meta:$jooqVersion")
    api("org.jooq:jooq-kotlin:$jooqVersion")
    api("org.jooq:jooq-jackson-extensions:$jooqVersion")

    jooqGenerator("org.jooq:jooq:$jooqVersion")
    jooqGenerator("org.jooq:jooq-meta:$jooqVersion")
    jooqGenerator("org.postgresql:postgresql:42.7.1")

    liquibaseRuntime("org.liquibase:liquibase-core:4.25.1")
    liquibaseRuntime("info.picocli:picocli:4.6.3")
    liquibaseRuntime("org.postgresql:postgresql:42.7.1")

    implementation("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
    implementation("com.fasterxml.jackson.core:jackson-databind:$jacksonDatabindVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:$jacksonVersion")

    implementation("org.slf4j:jul-to-slf4j:2.0.11")

    implementation("com.google.code.findbugs:jsr305:3.0.2")

    implementation("io.swagger:swagger-annotations:$swaggerAnnotationsVersion")
    implementation("jakarta.annotation:jakarta.annotation-api:$jakartaAnnotationVersion")
    implementation("javax.annotation:javax.annotation-api:1.3.2")
    implementation("javax.validation:validation-api:2.0.1.Final")

    implementation("org.springframework.boot:spring-boot:3.2.1")
}

// Создаем контейнер PostgreSQL
val postgresService = project.gradle.sharedServices.registerIfAbsent(
    "postgres",
    PostgresService::class.java
) {}
val postgresContainer = postgresService.get().container

if (project.properties.containsKey("generateJooq")) {
    postgresContainer.start()
    liquibase {
        activities.register("main") {
            arguments = mapOf(
                "logLevel" to "info",
                "changeLogFile" to "src/main/resources/db/changelog/db.changelog-master.xml",
                "url" to postgresContainer.jdbcUrl,
                "username" to postgresContainer.username,
                "password" to postgresContainer.password,
                "classpath" to rootDir,
            )
        }
        runList = "main"
        jooq {
            version.set(jooqVersion)
            edition.set(JooqEdition.OSS)

            configurations {
                create("main") {
                    jooqConfiguration.apply {
                        logging = Logging.WARN
                        generateSchemaSourceOnCompilation = false

                        // Здесь указывается откуда Jooq достанет схему, чтобы сгенерировать код
                        if (postgresContainer.isRunning) {
                            jdbc.apply {
                                driver = "org.postgresql.Driver"
                                url = postgresContainer.jdbcUrl
                                user = postgresContainer.username
                                password = postgresContainer.password
                            }
                        } else {
                            jdbc.apply {
                                driver = "org.postgresql.Driver"
                                url = "http://localhost"
                                user = postgresContainer.username
                                password = postgresContainer.password
                            }
                        }

                        // Здесь указывается как конкретно генерить код
                        generator.apply {
                            name = "org.jooq.codegen.KotlinGenerator"
                            strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"

                            target.apply {
                                packageName = javaPackage
                                directory = "src/main/kotlin"  // default (can be omitted)
                            }

                            database.apply {
                                name = "org.jooq.meta.postgres.PostgresDatabase"
                                inputSchema = "public"
                                excludes = "databasechangelog|databasechangeloglock"
                                forcedTypes.apply {
                                    add(
                                        // Костыль для нормального типа Json-а из базы
                                        ForcedType()
                                            .withTypes("jsonb")
                                            .withJsonConverter(true)
                                            .withUserType("com.fasterxml.jackson.databind.JsonNode")
                                    )
                                }
                            }

                            generate.apply {
                                isDeprecated = false
                                isRecords = true
                                isDaos = true
                                isSpringAnnotations = true
                                isPojos = false
                                isKotlinNotNullPojoAttributes = true
                                isKotlinNotNullRecordAttributes = true
                                isKotlinNotNullInterfaceAttributes = true
                                isSpringAnnotations = true
                                isImmutablePojos = true
                                isPojosAsKotlinDataClasses = true
                                isFluentSetters = true
                                isComments = true
                            }
                        }
                    }
                }
            }
        }
    }


    val liquibaseUpdate = tasks.getByName("update", LiquibaseTask::class)
    val generateJooq = tasks.named<JooqGenerate>("generateJooq") {
        allInputsDeclared.set(true)
    }.get()
    generateJooq.group = "Code Generation"

    generateJooq.dependsOn(liquibaseUpdate)
}
// Настраиваем миграции liquibase на контейнер



val buildTask = tasks.named("build").get()
val compileJava = tasks.named("compileJava").get()
val sourcesJar = tasks.named("sourcesJar").get()

/**
 * С помощью данного сервиса мы поднимаем базу, на которую накатим миграции во время выполнения build-скрипта.
 * Это нужно, чтобы с помощью этой базы можно было сгенерировать код к JOOQ. Если не использовать этот подход, то тогда,
 * чтобы собрать проект, нужна будет локальная база.
 *
 * У JOOQ есть подход, когда он генерится с миграций напрямую минуя базу, но он использует только диалект H2, либо тогда
 * миграции надо писать на xml или yaml.
 *
 * Этот вариант самый нормальный и безболезненный из имеющихся.
 */
abstract class PostgresService : BuildService<BuildServiceParameters.None>, AutoCloseable {

    val container: PostgreSQLContainer<Nothing> = PostgreSQLContainer<Nothing>("postgres:14-alpine").apply {
        withDatabaseName("buildscript")
        withUsername("jooq")
        withPassword("jooq")
    }

    override fun close() {
        container.stop()
    }
}