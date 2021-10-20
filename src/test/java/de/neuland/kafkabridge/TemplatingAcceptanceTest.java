package de.neuland.kafkabridge;

import de.neuland.kafkabridge.test.EmbeddedKafkaConfiguration;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.HashSet;
import java.util.Set;

import static de.neuland.kafkabridge.ProductType.SPECIAL;
import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON_VALUE;
import static de.neuland.kafkabridge.test.EmbeddedKafkaConfiguration.*;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.KEY_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.writeString;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = { EmbeddedKafkaConfiguration.class })
class TemplatingAcceptanceTest {
    private static final Logger LOG = LoggerFactory.getLogger(TemplatingAcceptanceTest.class);
    private static final Set<Path> TEMPLATE_PATHS = new HashSet<>();

    private KafkaConsumer<ProductKey, Product> consumer;

    @Autowired private WebTestClient webTestClient;

    @BeforeEach
    void setUpKafkaConsumer() {
        var configs = kafkaConsumerConfiguration();
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configs.put(KEY_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class.getName());
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configs.put(VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class.getName());
        configs.put(SPECIFIC_AVRO_READER_CONFIG, true);
        consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(singleton(TemplatingAcceptanceTest.class.getSimpleName()));

        registerSchema(ProductKey.class.getName(), ProductKey.getClassSchema());
        registerSchema(Product.class.getName(), Product.getClassSchema());
    }


    @BeforeEach
    void registerSchemas() {
        registerSchema(ProductKey.class.getName(), ProductKey.getClassSchema());
        registerSchema(Product.class.getName(), Product.getClassSchema());
    }



    @AfterEach
    void cleanUpKafkaConsumer() {
        consumer.unsubscribe();
        consumer.close();
    }

    @AfterAll
    static void deleteTemplatePathFile() {
        TEMPLATE_PATHS.forEach(TemplatingAcceptanceTest::deleteIfExists);
    }

    @Test
    void shouldSendKafkaMessage() {
        // given
        var keyTemplatePath = givenTemplate("json", """
                {
                  "code": "default"
                }""");

        var valueTemplatePath = givenTemplate("json", """
                {
                  "type": "REGULAR",
                  "available_since": [(
                    ${ #temporals.createDateTime("2021-08-31T20:30:00").minusSeconds(5).atZone(utcZoneId).toInstant().toEpochMilli() }
                  )]
                }""");

        var topic = TemplatingAcceptanceTest.class.getSimpleName();
        var recordKeySchemaSubject = ProductKey.class.getName();
        var recordKey = "{}";
        var recordValueSchemaSubject = Product.class.getName();
        var recordValue = """
                {
                  "type": "SPECIAL",
                  "name": "Kafka Bridge Product"
                }""";

        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted(topic))
                                  .header("Key", recordKey)
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", recordKeySchemaSubject)
                                  .header("Key-Template-Path", keyTemplatePath.toString())
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", recordValueSchemaSubject)
                                  .header("Template-Path", valueTemplatePath.toString())
                                  .bodyValue(recordValue)
                                  .exchange();

        // then
        result.expectStatus()
              .isOk()
              .expectBody()
              .isEmpty();
        assertThat(pollAllRemainingRecords(consumer)).singleElement().satisfies(consumerRecord -> {
            assertThat(consumerRecord.key().getCode()).isEqualTo("default");
            assertThat(consumerRecord.value().getType()).isEqualTo(SPECIAL);
            assertThat(consumerRecord.value().getName()).isEqualTo("Kafka Bridge Product");
            assertThat(consumerRecord.value().getAvailableSince()).isEqualTo(Instant.parse("2021-08-31T20:29:55Z"));
        });
    }

    @Test
    void shouldHaveTemplateVariable() {
        // given
        var valueTemplatePath = givenTemplate("json", """
                [# th:with="type=${parameters.type} ?: 'REGULAR'" ]
                {
                  "type": "[( ${type} )]",
                  "available_since": [(
                    ${ #temporals.createDateTime("2021-08-31T20:30:00").minusSeconds(5).atZone(utcZoneId).toInstant().toEpochMilli() }
                  )]
                }
                [/]
                """);

        var recordKeySchemaSubject = ProductKey.class.getName();
        var recordKey = """
            {"code": "default"}""";
        var topic = TemplatingAcceptanceTest.class.getSimpleName();
        var recordValueSchemaSubject = Product.class.getName();
        var recordValue = """
                {
                  "name": "Kafka Bridge Product"
                }""";

        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted(topic))
                                  .header("Key", recordKey)
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", recordKeySchemaSubject)
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", recordValueSchemaSubject)
                                  .header("Template-Path", valueTemplatePath.toString())
                                  .header("Template-Parameter-type", "SPECIAL")
                                  .bodyValue(recordValue)
                                  .exchange();

        // then
        result.expectStatus()
              .isOk()
              .expectBody()
              .isEmpty();
        assertThat(pollAllRemainingRecords(consumer)).singleElement().satisfies(consumerRecord -> {
            assertThat(consumerRecord.key().getCode()).isEqualTo("default");
            assertThat(consumerRecord.value().getType()).isEqualTo(SPECIAL);
            assertThat(consumerRecord.value().getName()).isEqualTo("Kafka Bridge Product");
            assertThat(consumerRecord.value().getAvailableSince()).isEqualTo(Instant.parse("2021-08-31T20:29:55Z"));
        });
    }

    private Path givenTemplate(String fileExtension,
                               String content) {
        try {
            var templatePath = createTempFile("kafkabridge-" + TemplatingAcceptanceTest.class.getSimpleName(), "." + fileExtension);
            TEMPLATE_PATHS.add(templatePath);
            writeString(templatePath, content);
            return templatePath;
        } catch (IOException e) {
            throw new RuntimeException("Error on writing template", e);
        }
    }

    private static void deleteIfExists(Path templatePath) {
        try {
            Files.deleteIfExists(templatePath);
        } catch (IOException e) {
            LOG.error("Error deleting " + templatePath, e);
        }
    }
}
