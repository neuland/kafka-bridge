package de.neuland.kafkabridge.domain.schemaregistry;

import java.util.concurrent.CompletableFuture;

public interface AvroSchemaRepository {
    CompletableFuture<AvroSchema> findBySubject(Subject subject);
}
