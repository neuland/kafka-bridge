package de.neuland.kafkabridge.test;

import de.neuland.kafkabridge.application.ApplicationService;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webtestclient.autoconfigure.AutoConfigureWebTestClient;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.MOCK;


@SpringBootTest(webEnvironment = MOCK)
@AutoConfigureWebTestClient
public abstract class AbstractControllerTest {
    @Autowired protected WebTestClient webTestClient;
    @MockitoBean protected ApplicationService applicationService;

    @MockitoBean private SchemaRegistryClient schemaRegistryClient;
    @MockitoBean private KafkaProducer<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValueKafkaProducer;
    @MockitoBean private KafkaProducer<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValueKafkaProducer;
}
