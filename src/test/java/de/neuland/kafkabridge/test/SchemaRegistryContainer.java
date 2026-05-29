package de.neuland.kafkabridge.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.kafka.ConfluentKafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.stream.Collectors;

import static java.time.Duration.ofSeconds;


@SuppressWarnings("resource")
public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> {
    private static final int KAFKA_PORT = 9093;
    private static final int SCHEMA_REGISTRY_PORT = 8081;

    public SchemaRegistryContainer(DockerImageName dockerImageName) {
        super(dockerImageName);
        withExposedPorts(SCHEMA_REGISTRY_PORT);
        waitingFor(Wait.forHttp("/subjects")
                       .forStatusCode(200)
                       .withStartupTimeout(ofSeconds(60)));
    }

    public SchemaRegistryContainer withKafka(ConfluentKafkaContainer kafka) {
        var bootstrapServers = kafka.getNetworkAliases()
                                    .stream()
                                    .map(networkAlias -> "PLAINTEXT://%s:%d".formatted(networkAlias, KAFKA_PORT))
                                    .collect(Collectors.joining(","));

        return withKafka(kafka.getNetwork(),
                         bootstrapServers);
    }

    public SchemaRegistryContainer withKafka(Network network, String bootstrapServers) {
        withNetwork(network);
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT);
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", bootstrapServers);
        return self();
    }

    public String getUrl() {
        return "http://%s:%d".formatted(getHost(), getMappedPort(SCHEMA_REGISTRY_PORT));
    }
}
