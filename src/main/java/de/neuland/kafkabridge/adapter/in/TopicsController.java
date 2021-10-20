package de.neuland.kafkabridge.adapter.in;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.kafkabridge.application.ApplicationService;
import de.neuland.kafkabridge.application.ConvertAndPublishAvroKeyAvroValueCommand;
import de.neuland.kafkabridge.application.ConvertAndPublishCommand;
import de.neuland.kafkabridge.application.ConvertAndPublishStringKeyAvroValueCommand;
import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import de.neuland.kafkabridge.domain.schemaregistry.Subject;
import de.neuland.kafkabridge.lib.templating.ParameterKey;
import de.neuland.kafkabridge.lib.templating.ParameterValue;
import de.neuland.kafkabridge.lib.templating.TemplateRenderer;
import io.vavr.Tuple;
import io.vavr.Tuple2;
import io.vavr.collection.HashMap;
import io.vavr.collection.HashSet;
import io.vavr.collection.Map;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import io.vavr.control.Try;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.nio.file.Path;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON;
import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON_VALUE;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;


@RestController
@RequestMapping(path = "/topics")
public class TopicsController {
    public static final Pattern TEMPLATE_PARAMETER = Pattern.compile("Template-Parameter-(.+)");
    private final TemplateRenderer templateRenderer;
    private final ApplicationService applicationService;
    private final ObjectMapper objectMapper;

    public TopicsController(TemplateRenderer templateRenderer,
                            ApplicationService applicationService,
                            ObjectMapper objectMapper) {
        this.templateRenderer = templateRenderer;
        this.applicationService = applicationService;
        this.objectMapper = objectMapper;
    }

    @PostMapping(path = "/{topic}/send")
    public Mono<ResponseEntity<?>> send(@PathVariable("topic") Topic topic,
                                        @RequestHeader(name = "Key",
                                                       required = false) String key,
                                        @RequestHeader(name = "Key-Content-Type",
                                                       required = false) MediaType keyContentType,
                                        @RequestHeader(name = "Key-Schema-Subject",
                                                       required = false) Subject keySchemaSubject,
                                        @RequestHeader(name = "Key-Template-Path",
                                                       required = false) String maybeKeyTemplatePath,
                                        @RequestBody(required = false) String value,
                                        @RequestHeader(name = "Content-Type",
                                                       required = false) MediaType valueContentType,
                                        @RequestHeader(name = "Schema-Subject",
                                                       required = false) Subject valueSchemaSubject,
                                        @RequestHeader(name = "Template-Path",
                                                       required = false) String maybeValueTemplatePath,
                                        @RequestHeader HttpHeaders allHeaders) {

        if (value == null) {
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with a non-empty value is supported. Please send the value in the body."));
        }

        if (valueSchemaSubject == null) {
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with a value schema is supported. Please specify the 'Schema-Subject' header."));
        }

        if (!APPLICATION_AVRO_JSON.equals(valueContentType)) {
            return Mono.just(ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body(
                "Only sending of Kafka messages with an Avro schema value is supported. Please set the 'Content-Type' header to '" + APPLICATION_AVRO_JSON_VALUE + "'."));
        }

        if (key == null) {
            return Mono.just(ResponseEntity.badRequest().body("Only sending of Kafka messages with keys is supported. Please set the key in the 'Key' header."));
        }

        var templateParameters = HashMap.ofEntries(Stream.ofAll(allHeaders.entrySet())
                                                         .map(Tuple::fromEntry)
                                                         .flatMap(entry -> Stream.ofAll(entry._2)
                                                                                 .headOption()
                                                                                 .map(Tuple.of(entry._1)::append))
                                                         .flatMap(entry -> {
                                                             var matcher = TEMPLATE_PARAMETER.matcher(entry._1);
                                                             if (matcher.matches()) {
                                                                 return Option.of(entry.update1(matcher.group(1)));
                                                             }

                                                             return Option.none();
                                                         }))
                                        .bimap(ParameterKey::new,
                                               ParameterValue::new);

        final ConvertAndPublishCommand command;
        var recordValue = new RecordValue<>(asJson(value, maybeValueTemplatePath, templateParameters));

        if (APPLICATION_AVRO_JSON.equals(keyContentType)) {
            if (keySchemaSubject == null) {
                return Mono.just(ResponseEntity.badRequest().body(
                    "Only sending of Kafka messages with an Avro schema registered in a schema registry is supported. Please specify the 'Key-Schema-Subject' header."));
            }

            var recordKey = new RecordKey<>(asJson(key, maybeKeyTemplatePath, templateParameters));

            command = new ConvertAndPublishAvroKeyAvroValueCommand(topic,
                                                                   recordKey,
                                                                   recordValue,
                                                                   keySchemaSubject,
                                                                   valueSchemaSubject);
        } else if (keyContentType == null) {
            command = new ConvertAndPublishStringKeyAvroValueCommand(topic,
                                                                     new RecordKey<>(key),
                                                                     recordValue,
                                                                     valueSchemaSubject);
        } else {
            return Mono.just(ResponseEntity.status(UNSUPPORTED_MEDIA_TYPE).body(
                "Only sending of Kafka messages with a String key or an Avro schema key is supported. Please leave the 'Key-Content-Type' header empty or set it to '"
                + APPLICATION_AVRO_JSON_VALUE + "'."));
        }

        return Mono.fromFuture(applicationService.convertAndPublish(command)
                                                 .toCompletableFuture())
                   .map(__ -> ResponseEntity.ok().build());
    }

    private Json<JsonNode> asJson(String keyOrValue,
                                  String maybeTemplatePath,
                                  Map<ParameterKey, ParameterValue> templateParameters) {
        return Option.of(maybeTemplatePath)
                     .map(Path::of)
                     .fold(
                         () -> new Json<>(Try.of(() -> objectMapper.readTree(keyOrValue)).get()),
                         templatePath -> templateRenderer.render(templatePath,
                                                                 new Json<>(keyOrValue),
                                                                 templateParameters)
                     );
    }
}
