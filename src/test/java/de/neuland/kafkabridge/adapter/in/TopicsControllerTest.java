package de.neuland.kafkabridge.adapter.in;

import de.neuland.kafkabridge.test.AbstractControllerAcceptanceTest;
import org.junit.jupiter.api.Test;

import static de.neuland.kafkabridge.lib.http.MediaTypes.APPLICATION_AVRO_JSON_VALUE;
import static org.mockito.BDDMockito.then;
import static org.springframework.http.HttpStatus.UNSUPPORTED_MEDIA_TYPE;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

class TopicsControllerTest extends AbstractControllerAcceptanceTest {

    @Test
    void shouldHaveBadRequestIfKeyIsMissing() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", "<key-schema-subject>")
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", "<value-schema-subject>")
                                  .bodyValue("<value>")
                                  .exchange();

        // then
        result.expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with keys is supported. Please set the key in the 'Key' header.");

        then(applicationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldHaveBadRequestIfKeyContentTypeIsNeitherAbsentNorApplicationAvroJson() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key", "<key>")
                                  .header("Key-Content-Type", APPLICATION_JSON_VALUE)
                                  .header("Key-Schema-Subject", "<key-schema-subject>")
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", "<value-schema-subject>")
                                  .bodyValue("<value>")
                                  .exchange();

        // then
        result.expectStatus()
              .isEqualTo(UNSUPPORTED_MEDIA_TYPE)
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with a String key or an Avro schema key is supported. Please leave the 'Key-Content-Type' header empty or set it to 'application/avro+json'.");

        then(applicationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldHaveBadRequestIfKeySchemaSubjectIsMissingWhileKeyContentTypeImpliesSchemaRegistry() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key", "<key>")
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", "<value-schema-subject>")
                                  .bodyValue("<value>")
                                  .exchange();

        // then
        result.expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with an Avro schema registered in a schema registry is supported. Please specify the 'Key-Schema-Subject' header.");

        then(applicationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldHaveBadRequestIfValueIsMissing() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key", "<key>")
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", "<key-schema-subject>")
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Schema-Subject", "<value-schema-subject>")
                                  .bodyValue("")
                                  .exchange();

        // then
        result.expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with a non-empty value is supported. Please send the value in the body.");

        then(applicationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldHaveBadRequestIfValueContentTypeIsNotApplicationAvroJson() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key", "<key>")
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", "<key-schema-subject>")
                                  .header("Content-Type", APPLICATION_JSON_VALUE)
                                  .header("Schema-Subject", "<value-schema-subject>")
                                  .bodyValue("<value>")
                                  .exchange();

        // then
        result.expectStatus()
              .isEqualTo(UNSUPPORTED_MEDIA_TYPE)
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with an Avro schema value is supported. Please set the 'Content-Type' header to 'application/avro+json'.");

        then(applicationService).shouldHaveNoInteractions();
    }

    @Test
    void shouldHaveBadRequestIfValueSchemaSubjectIsMissing() {
        // when
        var result = webTestClient.post()
                                  .uri("/topics/%s/send".formatted("<topic>"))
                                  .header("Key", "<key>")
                                  .header("Key-Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .header("Key-Schema-Subject", "<key-schema-subject>")
                                  .header("Content-Type", APPLICATION_AVRO_JSON_VALUE)
                                  .bodyValue("<value>")
                                  .exchange();

        // then
        result.expectStatus()
              .isBadRequest()
              .expectBody(String.class)
              .isEqualTo("Only sending of Kafka messages with a value schema is supported. Please specify the 'Schema-Subject' header.");

        then(applicationService).shouldHaveNoInteractions();
    }
}
