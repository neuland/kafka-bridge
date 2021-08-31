package de.neuland.kafkabridge.domain.schemaregistry;

import io.vavr.concurrent.Future;

public interface AvroSchemaRepository {
    Future<AvroSchema> findBySubject(Subject subject);
}