package de.neuland.kafkabridge.lib.http;

import org.springframework.http.MediaType;

public interface MediaTypes {
    String APPLICATION_AVRO_JSON_VALUE = "application/avro+json";
    MediaType APPLICATION_AVRO_JSON = MediaType.parseMediaType(APPLICATION_AVRO_JSON_VALUE);
}
