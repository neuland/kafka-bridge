package de.neuland.kafkabridge.application;

import de.neuland.kafkabridge.domain.JsonString;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;

public record ConvertAndPublishStringKeyAvroValueCommand(Topic topic,
                                                         RecordKey<String> recordKey,
                                                         RecordValue<JsonString> recordValue,
                                                         Subject valueSchemaSubject) implements ConvertAndPublishCommand {
}
