import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.openapitools.generator.gradle.plugin.tasks.GenerateTask
import org.springframework.boot.gradle.plugin.ResolveMainClassName
import org.springframework.boot.gradle.tasks.bundling.BootJar


plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"

    id("org.openapi.generator") version "7.2.0"
    id("io.swagger.core.v3.swagger-gradle-plugin") version "2.2.20"
    id("nu.studer.jooq") version "9.0"
    id("org.liquibase.gradle") version "2.2.1"

    id("org.jlleitschuh.gradle.ktlint") version "12.1.1"
}

group = "ru.itmo.ict.todolist"
version = "1.0.0"

val oasSpecLocation = "docs/openapi/openapi.yaml"
val oasGenOutputDir = project.layout.buildDirectory.dir("generated-server").get()
val oasPackage = "ru.itmo.ict.todolist.generated"

val jooqVersion = "3.19.1"

java {
    sourceCompatibility = JavaVersion.VERSION_17
}

kotlin {
    sourceSets["main"].apply {
        kotlin.srcDir("$oasGenOutputDir/src/main/kotlin")
    }
}

repositories {
    mavenCentral()
    maven(url = uri("https://plugins.gradle.org/m2/"))
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
        classpath("org.jlleitschuh.gradle:ktlint-gradle:10.1.0")
    }
}

ktlint {
    verbose.set(true)
    debug.set(true)
    ignoreFailures.set(false)
    outputToConsole.set(true)
}

// Здесь объявляются зависимости самого приложения
dependencies {
    // Spring Boot dependencies
    implementation("org.springframework.boot:spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-jooq")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    testImplementation("org.springframework.boot:spring-boot-starter-test")

    // Jackson
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")

    // Database
    runtimeOnly("org.postgresql:postgresql")
    implementation("org.liquibase:liquibase-core")
    implementation("jakarta.persistence:jakarta.persistence-api:3.1.0")
    implementation(project(":jooq-src"))

    // Kotlin
    implementation(kotlin("stdlib"))
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Swagger generated code
    implementation("io.springfox:springfox-boot-starter:3.0.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.20")
    implementation("io.springfox:springfox-oas:3.0.0")
    compileOnly("javax.servlet:javax.servlet-api:4.0.1")
    implementation("org.springdoc:springdoc-openapi-ui:1.7.0")

    // Other
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("jakarta.annotation:jakarta.annotation-api:2.1.0")
    implementation("com.google.code.findbugs:jsr305:3.0.2")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    implementation("javax.validation:validation-api:2.0.1.Final")

    // Tests
    testImplementation("io.mockk:mockk:1.13.9")
    testImplementation("com.ninja-squad:springmockk:4.0.2")
    testImplementation("org.testcontainers:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:junit-jupiter")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs += "-Xjsr305=strict"
        jvmTarget = "17"
    }

    dependsOn("generateServer")
    dependsOn("generateHtmlSpec")
    dependsOn("generateMergedSpec")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// Настраиваем генерацию Jooq

// Генерируем код к нашему сервису по openapi сервиса
val generateServer =
    tasks.register("generateServer", GenerateTask::class) {
        dependsOn("runKtlintCheckOverMainSourceSet")
        delete(oasGenOutputDir.toString())

        input = project.file(oasSpecLocation).path
        outputDir.set(oasGenOutputDir.toString())
        modelPackage.set("$oasPackage.model")
        apiPackage.set("$oasPackage.api")
        packageName.set(oasPackage)

        // Как будет происходить генерация
        generatorName.set("kotlin-spring")
        configOptions.set(
            mapOf(
                "dateLibrary" to "java8",
                "interfaceOnly" to "true",
                "useTags" to "true",
                "useBeanValidation" to "true",
                "serviceInterface" to "false",
                "openApiNullable" to "false",
                "generateModelTests" to "true",
                "hideGenerationTimestamp" to "true",
                "useSpringBoot3" to "true",
            ),
        )

        // Удаляем не нужный сгенеренный хлам
        doLast {
            delete(
                "$oasGenOutputDir/.openapi-generator",
                "$oasGenOutputDir/.openapi-generator-ignore",
                "$oasGenOutputDir/build.gradle.kts",
                "$oasGenOutputDir/pom.xml",
                "$oasGenOutputDir/README.md",
                "$oasGenOutputDir/settings.gradle",
                "$oasGenOutputDir/src/main/kotlin/org/openapitools/api/Exceptions.kt",
            )
        }
    }

// Генерируем красивую документацию к нашей спеке
val generateHtmlSpec =
    tasks.register("generateHtmlSpec", GenerateTask::class) {
        dependsOn("runKtlintCheckOverMainSourceSet")
        generatorName.set("html2")
        input = project.file(oasSpecLocation).path
        outputDir.set(oasGenOutputDir.toString())

        // Удаляем не нужный сгенеренный хлам
        doLast {
            delete(
                "$oasGenOutputDir/.openapi-generator",
                "$oasGenOutputDir/.openapi-generator-ignore",
            )
        }
    }

val generateMergedSpec =
    tasks.register("generateMergedSpec", GenerateTask::class) {
        dependsOn("runKtlintCheckOverMainSourceSet")
        input = project.file(oasSpecLocation).path
        outputDir.set(oasGenOutputDir.toString())
        generatorName.set("openapi-yaml")
        configOptions.set(
            mapOf(
                "identifierNamingConvention" to "camel_case",
            ),
        )
    }

// С помощью этой таски мы перенесем html-спеку в ресурсы приложения, чтобы она попала в jar-ник
val copyOpenApiHtml =
    tasks.register<Copy>("copyOpenApiHtml") {
        dependsOn(generateHtmlSpec)
        dependsOn(generateServer)
        dependsOn(generateMergedSpec)
        dependsOn(copyOpenApiYaml)
        from(layout.buildDirectory.dir("$oasGenOutputDir/index.html"))
        into(layout.buildDirectory.dir("resources/main/static"))
    }

// С помощью этой таски мы перенесем yaml-спеку в ресурсы приложения, чтобы она попала в jar-ник
val copyOpenApiYaml =
    tasks.register<Copy>("copyOpenApiYaml") {
        dependsOn(generateHtmlSpec)
        dependsOn(generateServer)
        dependsOn(generateMergedSpec)
        from(layout.buildDirectory.dir("$oasGenOutputDir/openapi/openapi.yaml"))
        into(layout.buildDirectory.dir("resources/main/static"))
    }

val bootJarTask: BootJar = tasks.bootJar.get()
bootJarTask.dependsOn(copyOpenApiHtml)

val resolveMainClassName: ResolveMainClassName = tasks.resolveMainClassName.get()
resolveMainClassName.dependsOn(copyOpenApiHtml)

var jarTask = tasks.jar.get()
jarTask.dependsOn(copyOpenApiHtml)

tasks.test.get().dependsOn(copyOpenApiHtml)
