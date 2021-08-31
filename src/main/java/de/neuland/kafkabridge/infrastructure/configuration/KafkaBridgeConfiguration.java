package de.neuland.kafkabridge.infrastructure.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka-bridge")
public class KafkaBridgeConfiguration {
    private final Map<String, Object> kafka = new HashMap<>();
    private final Map<String, Object> schemaRegistry = new HashMap<>();

    public Map<String, Object> getKafka() {
        return kafka;
    }

    public Map<String, Object> getSchemaRegistry() {
        return schemaRegistry;
    }
}
