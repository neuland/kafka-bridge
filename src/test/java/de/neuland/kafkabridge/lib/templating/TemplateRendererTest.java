package de.neuland.kafkabridge.lib.templating;

import de.neuland.kafkabridge.domain.JsonString;
import de.neuland.kafkabridge.infrastructure.json.ObjectMapperFactory;
import de.neuland.kafkabridge.infrastructure.templating.TemplateEngineFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.writeString;
import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;

class TemplateRendererTest {
    private static final Logger LOG = LoggerFactory.getLogger(TemplateRendererTest.class);
    private static final Set<Path> TEMPLATE_PATHS = new HashSet<>();

    private final TemplateRenderer templateRenderer = new TemplateRenderer(new TemplateEngineFactory().templateEngine(),
                                                                           new ObjectMapperFactory().objectMapper());

    @AfterAll
    static void deleteTemplatePathFile() {
        TEMPLATE_PATHS.forEach(TemplateRendererTest::deleteIfExists);
    }

    @Test
    void shouldHaveMergedResult() {
        // given
        var templatePath = givenTemplate("json", """
                {
                  "date": [(
                    ${ #temporals.createDateTime("2021-08-31T20:30:00").atZone(systemDefaultZoneId).minusSeconds(5).toInstant().toEpochMilli() }
                  )]
                }""");

        // when
        var result = templateRenderer.render(templatePath,
                                             new JsonString("""
                                                                    {
                                                                      "id": "1234"
                                                                    }"""));

        assertThatJson(result.value()).isEqualTo("""
                                                         {
                                                           "date": 1630434595000,
                                                           "id": "1234"
                                                         }""");
    }

    private Path givenTemplate(String fileExtension,
                               String content) {
        try {
            var templatePath = createTempFile("kafkabridge-" + TemplateRendererTest.class.getSimpleName(), "." + fileExtension);
            TEMPLATE_PATHS.add(templatePath);
            writeString(templatePath, content);
            return templatePath;
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
