import org.jetbrains.kotlin.gradle.plugin.extraProperties
import java.util.Properties

plugins {
    kotlin("jvm") version "2.2.10"
    kotlin("plugin.spring") version "1.9.25"
    id("org.springframework.boot") version "3.5.4"
    id("io.spring.dependency-management") version "1.1.7"
    id("org.hibernate.orm") version "6.6.22.Final"
    id("org.graalvm.buildtools.native") version "0.10.6"
    kotlin("plugin.jpa") version "2.2.10"
    // Disable Sentry for now
//    id("io.sentry.jvm.gradle") version "5.9.0"
}

group = "camp.cultr"
version = "0.0.1-SNAPSHOT"
description = "DarakServer"

val buildEnv = Properties().apply {
    file("buildenv.gradle.properties").inputStream().use(::load)
    forEach { (k, v) -> project.extensions.extraProperties["$k"] = v }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
//    Spring boot
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    implementation("org.jetbrains.kotlin:kotlin-reflect")
//    3rd party libraries
    implementation("commons-codec:commons-codec:1.19.0")
    implementation("dev.turingcomplete:kotlin-onetimepassword:2.4.1")
    implementation("com.webauthn4j:webauthn4j-core:0.29.5.RELEASE")
    implementation("io.jsonwebtoken:jjwt-api:0.12.6")
    implementation("org.apache.tika:tika-core:3.2.2")
//    Dev libs
    developmentOnly("org.springframework.boot:spring-boot-devtools")
//    Database
    runtimeOnly("org.postgresql:postgresql")
//    3rd party libraries
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.6")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.6")
//    Testing
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit5")
    testImplementation("org.springframework.security:spring-security-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.addAll("-Xjsr305=strict")
    }
}

hibernate {
    enhancement {
        enableAssociationManagement = true
    }
}

allOpen {
    annotation("jakarta.persistence.Entity")
    annotation("jakarta.persistence.MappedSuperclass")
    annotation("jakarta.persistence.Embeddable")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
//sentry {
//    // Generates a JVM (Java, Kotlin, etc.) source bundle and uploads your source code to Sentry.
//    // This enables source context, allowing you to see your source
//    // code as part of your stack traces in Sentry.
//    includeSourceContext = true
//
//    org = "cultrcamp"
//    projectName = "darak-api-server"
//    authToken = project.extra["SENTRY_AUTH_TOKEN"] as String? ?: ""
//}


// 1) BuildConfig에 포함을 허용할 키만 화이트리스트
val allowedKeys = setOf(
    "SENTRY_DSN",
)

// 2) 문자열 이스케이프 유틸
fun escapeKotlinString(src: String) =
    src.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("$", "\\$")

// 3) 키를 Kotlin 상수명으로 정규화(영문/숫자/언더스코어만, 숫자로 시작 금지)
fun toConstName(key: String): String {
    val cleaned = key.uppercase().replace(Regex("[^A-Z0-9_]"), "_")
    return if (cleaned.firstOrNull()?.isDigit() == true) "_$cleaned" else cleaned
}

tasks.register("generateBuildConfig") {
    val outDir = layout.buildDirectory.dir("generated/source/buildConfig/main")
    val pkg = "camp.cultr.darakserver" // ← 프로젝트 패키지명 정확히
    outputs.dir(outDir)

    // 입력 선언(증분/캐시를 위해): 허용 키만 입력으로 잡기
    val inputsMap = project.ext.properties
        .filterKeys { it in allowedKeys }
    inputs.properties(inputsMap)

    doLast {
        val dest = outDir.get().file("BuildConfig.kt").asFile
        dest.parentFile.mkdirs()

        val lines = buildString {
            appendLine("package $pkg")
            appendLine()
            appendLine("object BuildConfig {")
            if (inputsMap.isEmpty()) {
                appendLine("    // No allowed keys found.")
            } else {
                inputsMap.forEach { (k, v) ->
                    appendLine(
                        "    const val ${toConstName(k)} = \"${escapeKotlinString(v as String)}\""
                    )
                }
            }
            appendLine("}")
        }

        dest.writeText(lines)
        logger.lifecycle("Generated ${dest.relativeTo(project.projectDir)}")
    }
}

sourceSets.named("main") {
    java.srcDir(layout.buildDirectory.dir("generated/source/buildConfig/main"))
}

tasks.named("compileKotlin") {
    dependsOn("generateBuildConfig")
}
