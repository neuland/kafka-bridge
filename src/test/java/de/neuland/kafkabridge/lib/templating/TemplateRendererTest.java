package de.neuland.kafkabridge.lib.templating;

import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.infrastructure.configuration.KafkaBridgeConfiguration;
import de.neuland.kafkabridge.infrastructure.json.ObjectMapperFactory;
import de.neuland.kafkabridge.infrastructure.templating.TemplateEngineFactory;
import io.vavr.control.Option;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.writeString;
import static java.time.Duration.ofSeconds;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.mockito.BDDMockito.given;


@ExtendWith(MockitoExtension.class)
class TemplateRendererTest {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateRendererTest.class);
    private static final Set<Path> TEMPLATE_PATHS = new HashSet<>();

    @Mock private KafkaBridgeConfiguration configuration;

    @AfterAll
    static void deleteTemplatePathFile() {
        TEMPLATE_PATHS.forEach(TemplateRendererTest::deleteIfExists);
    }

    @Test
    void shouldHaveMergedResult() {
        // given
        var templateRenderer = givenTemplateRendererWithoutCaching();

        var templatePath = createTemplate("json", """
            {
              "date": [(
                ${ #temporals.createDateTime("2021-08-31T20:30:00").atZone(systemDefaultZoneId).minusSeconds(5).toInstant().toEpochMilli() }
              )]
            }""");

        // when
        var result = templateRenderer.render(templatePath,
                                             new Json<>("""
                                                            {
                                                              "id": "1234"
                                                            }"""));

        var dateInEpochMilliseconds = LocalDateTime.parse("2021-08-31T20:29:55")
                                                   .atZone(ZoneId.systemDefault())
                                                   .toInstant()
                                                   .toEpochMilli();
        assertThatJson(result.value()).isEqualTo("""
                                                     {
                                                       "date": %d,
                                                       "id": "1234"
                                                     }""".formatted(dateInEpochMilliseconds));
    }

    @Test
    void shouldNotCacheTemplates() {
        // given
        var templateRenderer = givenTemplateRendererWithoutCaching();

        var templatePath = createTemplate("json", """
            {
              "data": "before"
            }""");

        templateRenderer.render(templatePath,
                                new Json<>("{}"));

        givenTemplateContent(templatePath, """
            {
              "data": "after"
            }""");

        // when
        var result = templateRenderer.render(templatePath,
                                             new Json<>("{}"));

        assertThatJson(result.value()).isEqualTo("""
                                                     {
                                                       "data": "after"
                                                     }""");
    }

    @Test
    void shouldCacheTemplates() {
        // given
        var templateRenderer = givenTemplateRendererWithCaching();

        var templatePath = createTemplate("json", """
            {
              "data": "before"
            }""");

        templateRenderer.render(templatePath,
                                new Json<>("{}"));

        givenTemplateContent(templatePath, """
            {
              "data": "after"
            }""");

        // when
        var result = templateRenderer.render(templatePath,
                                             new Json<>("{}"));

        assertThatJson(result.value()).isEqualTo("""
                                                     {
                                                       "data": "before"
                                                     }""");
    }

    private TemplateRenderer givenTemplateRendererWithoutCaching() {
        return givenTemplateRenderer(Option.none());
    }

    private TemplateRenderer givenTemplateRendererWithCaching() {
        return givenTemplateRenderer(Option.of(ofSeconds(30)));
    }

    private TemplateRenderer givenTemplateRenderer(Option<Duration> maybeCacheDuration) {
        given(configuration.getMaybeTemplateDirectory()).willReturn(Option.none());
        given(configuration.getMaybeTemplateCacheDuration()).willReturn(maybeCacheDuration);

        return new TemplateRenderer(new TemplateEngineFactory().templateEngine(configuration),
                                    new ObjectMapperFactory().objectMapper());
    }

    private Path createTemplate(String fileExtension,
                                String content) {
        try {
            var templatePath = createTempFile("kafkabridge-" + TemplateRendererTest.class.getSimpleName(), "." + fileExtension);
            TEMPLATE_PATHS.add(templatePath);
            givenTemplateContent(templatePath, content);
            return templatePath;
        } catch (IOException e) {
            throw new RuntimeException("Error on writing template", e);
        }
    }

    private void givenTemplateContent(Path templatePath,
                                      String content) {
        try {
            writeString(templatePath, content);
        } catch (IOException e) {
            throw new RuntimeException("Error on writing template", e);
        }
    }

    private static void deleteIfExists(Path templatePath) {
        try {
            Files.deleteIfExists(templatePath);
        } catch (IOException e) {
            LOG.error("Error deleting " + templatePath, e);
        }
    }
}
