package de.neuland.kafkabridge.test;

import de.neuland.kafkabridge.application.ApplicationService;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(webEnvironment = RANDOM_PORT)
public abstract class AbstractControllerAcceptanceTest {

    @Autowired protected WebTestClient webTestClient;
    @MockBean protected ApplicationService applicationService;

    @MockBean private SchemaRegistryClient schemaRegistryClient;
    @MockBean private KafkaProducer<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValueKafkaProducer;
    @MockBean private KafkaProducer<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValueKafkaProducer;
}
