package de.neuland.kafkabridge.application;

import com.fasterxml.jackson.databind.JsonNode;
import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;

public record ConvertAndPublishStringKeyAvroValueCommand(Topic topic,
                                                         RecordKey<String> recordKey,
                                                         RecordValue<Json<JsonNode>> recordValue,
                                                         Subject valueSchemaSubject) implements ConvertAndPublishCommand {
}
