package de.neuland.kafkabridge.test;


import io.confluent.kafka.schemaregistry.avro.AvroSchema;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.serializers.subject.RecordNameStrategy;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.Schema;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.util.StopWatch;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.StreamSupport;

import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.SCHEMA_REGISTRY_URL_CONFIG;
import static io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig.VALUE_SUBJECT_NAME_STRATEGY;
import static org.apache.kafka.clients.CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.AUTO_OFFSET_RESET_CONFIG;
import static org.apache.kafka.clients.consumer.ConsumerConfig.GROUP_ID_CONFIG;


@TestConfiguration
@Slf4j
public class EmbeddedKafkaConfiguration {
    private static final String CONFLUENT_PLATFORM_VERSION = "8.2.0";
    private static final ConfluentKafkaContainer kafka = new ConfluentKafkaContainer(DockerImageName.parse("confluentinc/cp-kafka")
                                                                                                    .withTag(CONFLUENT_PLATFORM_VERSION));
    private static final SchemaRegistryContainer schemaRegistry = new SchemaRegistryContainer(DockerImageName.parse("confluentinc/cp-schema-registry")
                                                                                                             .withTag(CONFLUENT_PLATFORM_VERSION));

    private static final AtomicReference<SchemaRegistryClient> schemaRegistryClientStore = new AtomicReference<>();

    public EmbeddedKafkaConfiguration() {
        setUpKafka();
    }

    private static void setUpKafka() {
        var stopWatch = new StopWatch(EmbeddedKafkaConfiguration.class.getSimpleName());

        startContainer(stopWatch, kafka.withNetwork(Network.newNetwork())
                                       .withNetworkAliases("kafka"));
        startContainer(stopWatch, schemaRegistry.withKafka(kafka));
        LOG.info(stopWatch.toString());

        System.setProperty("kafka-bridge.kafka.[bootstrap.servers]", kafka.getBootstrapServers());
        System.setProperty("kafka-bridge.schema-registry.url", schemaRegistry.getUrl());

        schemaRegistryClientStore.set(new CachedSchemaRegistryClient(schemaRegistry.getUrl(),
                                                                     100,
                                                                     commonConfigs()));
    }

    private static void startContainer(StopWatch stopWatch, GenericContainer<?> container) {
        stopWatch.start(container.getClass().getSimpleName());
        container.start();
        stopWatch.stop();

        var taskInfo = stopWatch.lastTaskInfo();
        LOG.info("{} started in {} ms",
                 taskInfo.getTaskName(),
                 taskInfo.getTimeMillis());
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
