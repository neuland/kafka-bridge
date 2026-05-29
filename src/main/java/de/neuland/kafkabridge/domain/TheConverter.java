package de.neuland.kafkabridge.domain;

import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;

import java.io.IOException;

public interface TheConverter<J> {
    SchemaRegistryAvroSerializedDataForKafka convert(Json<J> json,
                                                     AvroSchema avroSchema) throws IOException;
}
