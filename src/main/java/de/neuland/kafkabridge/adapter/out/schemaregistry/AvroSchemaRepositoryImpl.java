package de.neuland.kafkabridge.adapter.out.schemaregistry;

import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;
import de.neuland.kafkabridge.domain.schemaregistry.AvroSchemaRepository;
import de.neuland.kafkabridge.domain.schemaregistry.SchemaId;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.vavr.concurrent.Future;
import org.springframework.stereotype.Repository;

@Repository
public class AvroSchemaRepositoryImpl implements AvroSchemaRepository {
    private final SchemaRegistryClient schemaRegistryClient;

    public AvroSchemaRepositoryImpl(SchemaRegistryClient schemaRegistryClient) {
        this.schemaRegistryClient = schemaRegistryClient;
    }

    @Override
    public Future<AvroSchema> findBySubject(Subject subject) {
        return Future.of(() -> schemaRegistryClient.getLatestSchemaMetadata(subject.value()))
                     .mapTry(latestSchemaMetadata -> {
                         var schemaId = new SchemaId(latestSchemaMetadata.getId());
                         var parsedSchema = schemaRegistryClient.getSchemaBySubjectAndId(subject.value(), latestSchemaMetadata.getId());
                         var avroSchema = (io.confluent.kafka.schemaregistry.avro.AvroSchema) parsedSchema;
                         return new AvroSchema(schemaId, subject, avroSchema.rawSchema());
                     });
    }
}
