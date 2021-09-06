package de.neuland.kafkabridge.application;

import com.fasterxml.jackson.databind.JsonNode;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import de.neuland.kafkabridge.domain.TheConverter;
import de.neuland.kafkabridge.domain.kafka.Publisher;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.schemaregistry.AvroSchemaRepository;
import io.vavr.API;
import io.vavr.concurrent.Future;
import org.springframework.stereotype.Service;

import static java.util.function.Function.identity;

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

    public Future<Void> convertAndPublish(ConvertAndPublishCommand command) {
        if (command instanceof ConvertAndPublishStringKeyAvroValueCommand convertAndPublishStringKeyAvroValueCommand) {
            return convertAndPublish(convertAndPublishStringKeyAvroValueCommand);
        } else if (command instanceof ConvertAndPublishAvroKeyAvroValueCommand convertAndPublishAvroKeyAvroValueCommand) {
            return convertAndPublish(convertAndPublishAvroKeyAvroValueCommand);
        } else {
            throw new IllegalStateException();
        }
    }

    private Future<Void> convertAndPublish(ConvertAndPublishStringKeyAvroValueCommand command) {
        return avroSchemaRepository.findBySubject(command.valueSchemaSubject())
                                   .flatMap(avroSchema -> Future.fromTry(theConverter.convert(command.recordValue().value(), avroSchema)))
                                   .map(RecordValue::new)
                                   .flatMap(recordValue -> stringKeyAvroValuePublisher.send(command.topic(),
                                                                                            command.recordKey(),
                                                                                            recordValue));
    }

    private Future<Void> convertAndPublish(ConvertAndPublishAvroKeyAvroValueCommand command) {
        var eventualRecordKey = avroSchemaRepository.findBySubject(command.keySchemaSubject())
                                                    .flatMap(avroSchema -> Future.fromTry(theConverter.convert(command.recordKey().value(), avroSchema)))
                                                    .map(RecordKey::new);
        var eventualRecordValue = avroSchemaRepository.findBySubject(command.valueSchemaSubject())
                                                      .flatMap(avroSchema -> Future.fromTry(theConverter.convert(command.recordValue().value(), avroSchema)))
                                                      .map(RecordValue::new);
        return API.For(eventualRecordKey, eventualRecordValue)
                  .yield((recordKey, recordValue) -> avroKeyAvroValuePublisher.send(command.topic(),
                                                                                    recordKey,
                                                                                    recordValue))
                  .flatMap(identity());
    }
}
