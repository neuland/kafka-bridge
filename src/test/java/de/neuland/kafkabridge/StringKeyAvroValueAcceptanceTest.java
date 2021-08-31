package de.neuland.kafkabridge;

import de.neuland.kafkabridge.test.EmbeddedKafkaConfiguration;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.reactive.server.WebTestClient;

import java.time.Instant;

import static de.neuland.kafkabridge.ProductType.REGULAR;
import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON_VALUE;
import static de.neuland.kafkabridge.test.EmbeddedKafkaConfiguration.*;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static io.confluent.kafka.serializers.KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG;
import static java.util.Collections.singleton;
import static org.apache.kafka.clients.consumer.ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;


@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = { EmbeddedKafkaConfiguration.class })
class StringKeyAvroValueAcceptanceTest {

    private KafkaConsumer<String, Product> consumer;

    @Autowired
    private WebTestClient webTestClient;

    @BeforeEach
    void setUpKafkaConsumer() {
        var configs = kafkaConsumerConfiguration();
        configs.put(KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configs.put(VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        configs.put(VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class.getName());
        configs.put(SPECIFIC_AVRO_READER_CONFIG, true);
        consumer = new KafkaConsumer<>(configs);
        consumer.subscribe(singleton(StringKeyAvroValueAcceptanceTest.class.getSimpleName()));
    }

    @AfterEach
    void cleanUpKafkaConsumer() {
        consumer.unsubscribe();
        consumer.close();
    }

    @Test
    void shouldSendKafkaMessage() {
        // given
        registerSchema(Product.class.getName(), Product.getClassSchema());

        var topic = StringKeyAvroValueAcceptanceTest.class.getSimpleName();
        var recordKey = "Product-0001";
        var recordValueSchemaSubject = Product.class.getName();
        var recordValue = """
                {
                  "type": "REGULAR",
                  "name": "Kafka Bridge Product",
                  "available_since": %d
                }""".formatted(Instant.parse("2021-08-21T15:17:34Z").toEpochMilli());

        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted(topic))
                                  .header("Key", recordKey)
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", recordValueSchemaSubject)
                                  .bodyValue(recordValue)
                                  .exchange();

        // then
        result.expectStatus()
              .isOk()
              .expectBody()
              .isEmpty();
        assertThat(pollAllRemainingRecords(consumer)).singleElement().satisfies(consumerRecord -> {
            assertThat(consumerRecord.key()).isEqualTo("Product-0001");
            assertThat(consumerRecord.value().getType()).isEqualTo(REGULAR);
            assertThat(consumerRecord.value().getName()).isEqualTo("Kafka Bridge Product");
            assertThat(consumerRecord.value().getAvailableSince()).isEqualTo(Instant.parse("2021-08-21T15:17:34Z"));
        });
    }
}
