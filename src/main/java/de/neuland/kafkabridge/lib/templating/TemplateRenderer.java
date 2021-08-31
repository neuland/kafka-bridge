package de.neuland.kafkabridge.lib.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.kafkabridge.domain.JsonString;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.function.Function;

import static java.time.ZoneOffset.UTC;

@Component
public class TemplateRenderer {
    private final ITemplateEngine templateEngine;
    private final ObjectMapper objectMapper;

    public TemplateRenderer(ITemplateEngine templateEngine,
                            ObjectMapper objectMapper) {
        this.templateEngine = templateEngine;
        this.objectMapper = objectMapper;
    }

    public JsonString render(Path templatePath,
                             JsonString input) {
        return merge(new JsonString(render(templatePath)), input);
    }

    public String render(Path templatePath) {
        var context = new Context();
        context.setVariable("utcZoneId", UTC);
        context.setVariable("systemDefaultZoneId", ZoneId.systemDefault());
        context.setVariable("foo", (Function<LocalDateTime, String>) localDateTime -> Long.toString(localDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()));
        return templateEngine.process(templatePath.toString(), context);
    }

    public JsonString merge(JsonString baseString,
                            JsonString inputString) {
        try {
            var base = objectMapper.readTree(baseString.value());
            var input = objectMapper.readTree(inputString.value());
            var output = objectMapper.updateValue(base, input);
            var outputString = objectMapper.writeValueAsString(output);
            return new JsonString(outputString);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error merging JSON", e);
        }
    }
}
