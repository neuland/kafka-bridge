package de.neuland.kafkabridge.infrastructure.schemaregistry;

import de.neuland.kafkabridge.infrastructure.configuration.KafkaBridgeConfiguration;
import io.confluent.kafka.schemaregistry.client.CachedSchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SchemaRegistryClientFactory {
    private static final int DEFAULT_IDENTITY_MAP_CAPACITY = 100;

    @Bean
    public SchemaRegistryClient schemaRegistryClient(KafkaBridgeConfiguration kafkaBridgeConfiguration) {
        return new CachedSchemaRegistryClient((String) kafkaBridgeConfiguration.getSchemaRegistry().get("url"),
                                              DEFAULT_IDENTITY_MAP_CAPACITY,
                                              kafkaBridgeConfiguration.getSchemaRegistry());
    }
}
