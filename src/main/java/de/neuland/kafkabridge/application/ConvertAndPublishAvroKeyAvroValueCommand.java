package de.neuland.kafkabridge.application;

import com.fasterxml.jackson.databind.JsonNode;
import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;

public record ConvertAndPublishAvroKeyAvroValueCommand(Topic topic,
                                                       RecordKey<Json<JsonNode>> recordKey,
                                                       RecordValue<Json<JsonNode>> recordValue,
                                                       Subject keySchemaSubject,
                                                       Subject valueSchemaSubject) implements ConvertAndPublishCommand {
}
