package de.neuland.kafkabridge.infrastructure.configuration;

import io.vavr.control.Option;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

@Component
@ConfigurationProperties(prefix = "kafka-bridge")
public class KafkaBridgeConfiguration {
    private final Map<String, Object> kafka = new HashMap<>();
    private final Map<String, Object> schemaRegistry = new HashMap<>();
    private String templateDirectory;

    public Map<String, Object> getKafka() {
        return kafka;
    }

    public Map<String, Object> getSchemaRegistry() {
        return schemaRegistry;
    }

    public Option<Path> getMaybeTemplateDirectory() {
        return Option.of(templateDirectory).map(Path::of);
    }

    public void setTemplateDirectory(String templateDirectory) {
        this.templateDirectory = templateDirectory;
    }
}
