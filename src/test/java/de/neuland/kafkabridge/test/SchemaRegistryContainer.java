package de.neuland.kafkabridge.test;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.Network;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.utility.DockerImageName;

import static java.time.Duration.ofSeconds;

public class SchemaRegistryContainer extends GenericContainer<SchemaRegistryContainer> {

    public static final int SCHEMA_REGISTRY_PORT = 8081;

    public SchemaRegistryContainer(String version) {
        super(DockerImageName.parse("confluentinc/cp-schema-registry")
                             .withTag(version));
        withExposedPorts(SCHEMA_REGISTRY_PORT);
        waitingFor(Wait.forHttp("/subjects")
                       .forStatusCode(200)
                       .withStartupTimeout(ofSeconds(20)));
    }

    public SchemaRegistryContainer withKafka(KafkaContainer kafka) {
        return withKafka(kafka.getNetwork(),
                         kafka.getNetworkAliases().get(0) + ":" + 9092);
    }

    public SchemaRegistryContainer withKafka(Network network, String bootstrapServers) {
        withNetwork(network);
        withEnv("SCHEMA_REGISTRY_HOST_NAME", "schema-registry");
        withEnv("SCHEMA_REGISTRY_LISTENERS", "http://0.0.0.0:" + SCHEMA_REGISTRY_PORT);
        withEnv("SCHEMA_REGISTRY_KAFKASTORE_BOOTSTRAP_SERVERS", "PLAINTEXT://" + bootstrapServers);
        return self();
    }

    public String getUrl() {
        return "http://" + getContainerIpAddress() + ":" + getMappedPort(SCHEMA_REGISTRY_PORT);
    }
}
