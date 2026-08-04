plugins {
    alias(libs.plugins.spring.boot)
    alias(libs.plugins.spring.dependency.management)
    alias(libs.plugins.avro)
    java
}

group = "de.neuland-bfi.kafka-bridge"
version = "0.0.10"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(25)
    }
}

dependencies {
    annotationProcessor(libs.lombok)
    annotationProcessor(libs.spring.boot.configuration.processor)

    implementation(libs.spring.boot.starter.webflux)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.log4j2)
    implementation(libs.commons.lang3)
    implementation(libs.kafka.clients)
    implementation(libs.lombok)
    implementation(libs.confluent.schema.registry.client)
    implementation(libs.jackson.dataformat.avro)
    implementation(libs.thymeleaf)
    implementation(libs.thymeleaf.extras.java8time)
    implementation(libs.avro)


    testAnnotationProcessor(libs.lombok)
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.webtestclient)
    testImplementation(libs.reactor.test)
    testImplementation(libs.testcontainers)
    testImplementation(libs.testcontainers.kafka)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.confluent.kafka.avro.serializer)
    testImplementation(libs.json.unit.assertj)

  modules {
    module("org.springframework.boot:spring-boot-starter-logging") {
      replacedBy("org.springframework.boot:spring-boot-starter-log4j2", "Use Log4j2 instead of Logback")
    }
  }
}

avro {
    stringType.set("String")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

tasks.bootBuildImage {
    imageName.set("ghcr.io/neuland/kafka-bridge:${findProperty("containerImageTag") ?: "latest"}")
    publish.set(findProperty("publishImage")?.toString()?.toBoolean() ?: false)
    docker {
        publishRegistry {
            username.set(findProperty("registryUsername")?.toString() ?: System.getenv("REGISTRY_USERNAME") ?: "")
            password.set(findProperty("registryPassword")?.toString() ?: System.getenv("REGISTRY_PASSWORD") ?: "")
        }
    }
}
