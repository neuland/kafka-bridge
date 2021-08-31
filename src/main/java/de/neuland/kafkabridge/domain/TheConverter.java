package de.neuland.kafkabridge.domain;

import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;
import io.vavr.control.Try;

public interface TheConverter {
    Try<SchemaRegistryAvroSerializedDataForKafka> convert(JsonString jsonString,
                                                          AvroSchema avroSchema);
}