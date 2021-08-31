package de.neuland.kafkabridge.test;

import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.test.context.TestConfiguration;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.lifecycle.Startables;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;
import static org.slf4j.LoggerFactory.getLogger;


@TestConfiguration
public class EmbeddedKafkaConfiguration {
    private static final String CONFLUENT_PLATFORM_VERSION = "6.0.1";
    private static final KafkaContainer kafka = new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka")
                                                                                  .withTag(CONFLUENT_PLATFORM_VERSION));
    private static final SchemaRegistryContainer schemaRegistry = new SchemaRegistryContainer(CONFLUENT_PLATFORM_VERSION);

    private static final AtomicReference<SchemaRegistryClient> schemaRegistryClientStore = new AtomicReference<>();

    public EmbeddedKafkaConfiguration() {
        setUpKafka();
    }

    private static void setUpKafka() {
        if (kafka.isRunning()) {
            return;
        }

        kafka.withLogConsumer(new Slf4jLogConsumer(getLogger(KafkaContainer.class)))
             .withNetwork(Network.newNetwork())
             .withEmbeddedZookeeper();

        schemaRegistry.withLogConsumer(new Slf4jLogConsumer(getLogger(SchemaRegistryContainer.class)))
                      .withKafka(kafka);

        Startables.deepStart(Stream.of(kafka,
                                       schemaRegistry))
                  .join();

        System.setProperty("kafka-bridge.kafka.[bootstrap.servers]", kafka.getBootstrapServers());
        System.setProperty("kafka-bridge.schema-registry.url", schemaRegistry.getUrl());

        schemaRegistryClientStore.set(new CachedSchemaRegistryClient(schemaRegistry.getUrl(),
                                                                     100,
                                                                     commonConfigs()));
    }

    private static Map<String, Object> commonConfigs() {
        Map<String, Object> configs = new HashMap<>();
        // schema registry
        configs.put(SCHEMA_REGISTRY_URL_CONFIG, schemaRegistry.getUrl());
        configs.put(VALUE_SUBJECT_NAME_STRATEGY, RecordNameStrategy.class);

        // kafka
        configs.put(BOOTSTRAP_SERVERS_CONFIG, kafka.getBootstrapServers());

        return configs;
    }

    public static SchemaRegistryClient getSchemaRegistryClient() {
        setUpKafka();
        return schemaRegistryClientStore.get();
    }

    public static void registerSchema(String subject,
                                      Schema schema) {
        try {
            getSchemaRegistryClient().register(subject, new AvroSchema(schema));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Map<String, Object> kafkaConsumerConfiguration() {
        Map<String, Object> configs = commonConfigs();

        configs.put(GROUP_ID_CONFIG, "kafka-bridge-testing");
        configs.put(AUTO_OFFSET_RESET_CONFIG, "earliest");

        return configs;
    }

    public static <K, V> List<ConsumerRecord<K, V>> pollAllRemainingRecords(KafkaConsumer<K, V> consumer) {
        var consumerRecords = StreamSupport.stream(consumer.poll(Duration.ofSeconds(2)).spliterator(), false).toList();
        consumer.commitSync();
        return consumerRecords;
    }
}
