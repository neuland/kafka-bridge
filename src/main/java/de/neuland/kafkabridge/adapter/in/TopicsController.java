package de.neuland.kafkabridge.adapter.in;

import de.neuland.kafkabridge.application.ApplicationService;
import de.neuland.kafkabridge.application.ConvertAndPublishAvroKeyAvroValueCommand;
import de.neuland.kafkabridge.application.ConvertAndPublishCommand;
import de.neuland.kafkabridge.application.ConvertAndPublishStringKeyAvroValueCommand;
import de.neuland.kafkabridge.domain.JsonString;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON;
import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON_VALUE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;

@RestController
@RequestMapping(path = "/topics")
public class TopicsController {
    private final ApplicationService applicationService;

    public TopicsController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping(path = "/{topic}/send")
    public Mono<ResponseEntity<?>> send(@PathVariable("topic") Topic topic,
                                        @RequestHeader(name = "Key", required = false) String key,
                                        @RequestHeader(name = "Key-Content-Type", required = false) MediaType keyContentType,
                                        @RequestHeader(name = "Key-Schema-Subject", required = false) Subject keySchemaSubject,
                                        @RequestBody(required = false) String value,
                                        @RequestHeader(name = "Content-Type", required = false) MediaType valueContentType,
                                        @RequestHeader(name = "Schema-Subject", required = false) Subject valueSchemaSubject) {

        if (value == null)
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with a non-empty value is supported. Please send the value in the body."));

        if (valueSchemaSubject == null)
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with a value schema is supported. Please specify the 'Schema-Subject' header."));

        if (!APPLICATION_AVRO_JSON.equals(valueContentType))
            return Mono.just(ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body("Only sending of Kafka messages with an Avro schema value is supported. Please set the 'Content-Type' header to '" + APPLICATION_AVRO_JSON_VALUE + "'."));

        if (key == null)
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with keys is supported. Please set the key in the 'Key' header."));

        final ConvertAndPublishCommand command;

        if (APPLICATION_AVRO_JSON.equals(keyContentType)) {
            if (keySchemaSubject == null)
                return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with an Avro schema registered in a schema registry is supported. Please specify the 'Key-Schema-Subject' header."));

            command = new ConvertAndPublishAvroKeyAvroValueCommand(topic,
                                                                   new RecordKey<>(new JsonString(key)),
                                                                   new RecordValue<>(new JsonString(value)),
                                                                   keySchemaSubject,
                                                                   valueSchemaSubject);
        } else if (keyContentType == null) {
            command = new ConvertAndPublishStringKeyAvroValueCommand(topic,
                                                                     new RecordKey<>(key),
                                                                     new RecordValue<>(new JsonString(value)),
                                                                     valueSchemaSubject);
        } else {
            return Mono.just(ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body("Only sending of Kafka messages with a String key or an Avro schema key is supported. Please leave the 'Key-Content-Type' header empty or set it to '" + APPLICATION_AVRO_JSON_VALUE + "'."));
        }

        return Mono.fromFuture(applicationService.convertAndPublish(command)
                                                 .toCompletableFuture())
                   .map(__ -> ResponseEntity.ok().build());
    }
}
