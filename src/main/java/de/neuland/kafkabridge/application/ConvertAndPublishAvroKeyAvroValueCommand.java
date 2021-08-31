package de.neuland.kafkabridge.application;

import de.neuland.kafkabridge.domain.JsonString;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;

public record ConvertAndPublishAvroKeyAvroValueCommand(Topic topic,
                                                       RecordKey<JsonString> recordKey,
                                                       RecordValue<JsonString> recordValue,
                                                       Subject keySchemaSubject,
                                                       Subject valueSchemaSubject) implements ConvertAndPublishCommand {
}
