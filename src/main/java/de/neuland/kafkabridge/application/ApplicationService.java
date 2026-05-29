package de.neuland.kafkabridge.application;

import com.fasterxml.jackson.databind.JsonNode;
import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import de.neuland.kafkabridge.domain.TheConverter;
import de.neuland.kafkabridge.domain.kafka.Publisher;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;
import de.neuland.kafkabridge.domain.schemaregistry.AvroSchemaRepository;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

@Service
public class ApplicationService {
    private final AvroSchemaRepository avroSchemaRepository;
    private final TheConverter<JsonNode> theConverter;
    private final Publisher<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValuePublisher;
    private final Publisher<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValuePublisher;

    public ApplicationService(AvroSchemaRepository avroSchemaRepository,
                              TheConverter<JsonNode> theConverter,
                              Publisher<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValuePublisher,
                              Publisher<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValuePublisher) {
        this.avroSchemaRepository = avroSchemaRepository;
        this.theConverter = theConverter;
        this.stringKeyAvroValuePublisher = stringKeyAvroValuePublisher;
        this.avroKeyAvroValuePublisher = avroKeyAvroValuePublisher;
    }

    public CompletableFuture<Void> convertAndPublish(ConvertAndPublishCommand command) {
        if (command instanceof ConvertAndPublishStringKeyAvroValueCommand c) {
            return convertAndPublish(c);
        } else if (command instanceof ConvertAndPublishAvroKeyAvroValueCommand c) {
            return convertAndPublish(c);
        } else {
            throw new IllegalStateException();
        }
    }

    private CompletableFuture<Void> convertAndPublish(ConvertAndPublishStringKeyAvroValueCommand command) {
        return avroSchemaRepository.findBySubject(command.valueSchemaSubject())
                                   .thenCompose(avroSchema -> convert(command.recordValue().value(), avroSchema))
                                   .thenApply(RecordValue::new)
                                   .thenCompose(recordValue -> stringKeyAvroValuePublisher.send(command.topic(),
                                                                                               command.recordKey(),
                                                                                               recordValue));
    }

    private CompletableFuture<Void> convertAndPublish(ConvertAndPublishAvroKeyAvroValueCommand command) {
        var eventualRecordKey = avroSchemaRepository.findBySubject(command.keySchemaSubject())
                                                    .thenCompose(avroSchema -> convert(command.recordKey().value(), avroSchema))
                                                    .thenApply(RecordKey::new);
        var eventualRecordValue = avroSchemaRepository.findBySubject(command.valueSchemaSubject())
                                                      .thenCompose(avroSchema -> convert(command.recordValue().value(), avroSchema))
                                                      .thenApply(RecordValue::new);
        return eventualRecordKey
            .thenCombine(eventualRecordValue,
                         (recordKey, recordValue) -> avroKeyAvroValuePublisher.send(command.topic(),
                                                                                    recordKey,
                                                                                    recordValue))
            .thenCompose(Function.identity());
    }

    private CompletableFuture<SchemaRegistryAvroSerializedDataForKafka> convert(Json<JsonNode> json,
                                                                                 AvroSchema avroSchema) {
        try {
            return CompletableFuture.completedFuture(theConverter.convert(json, avroSchema));
        } catch (IOException e) {
            return CompletableFuture.failedFuture(e);
        }
    }
}
