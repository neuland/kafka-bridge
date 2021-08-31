package de.neuland.kafkabridge.domain.schemaregistry;

import org.apache.avro.Schema;

public record AvroSchema(SchemaId schemaId,
                         Subject subject,
                         Schema rawSchema) {
}