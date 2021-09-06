package de.neuland.kafkabridge.domain;

import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;
import io.vavr.control.Try;

public interface TheConverter<J> {
    Try<SchemaRegistryAvroSerializedDataForKafka> convert(Json<J> json,
                                                          AvroSchema avroSchema);
}
