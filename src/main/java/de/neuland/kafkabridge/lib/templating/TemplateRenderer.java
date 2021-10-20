package de.neuland.kafkabridge.lib.templating;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.neuland.kafkabridge.domain.Json;
import io.vavr.collection.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.context.Context;

import java.nio.file.Path;
import java.time.ZoneId;

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

    public Json<JsonNode> render(Path templatePath,
                                 Json<String> input,
                                 Map<ParameterKey, ParameterValue> parameters) {
        return merge(new Json<>(render(templatePath, parameters)), input);
    }

    public String render(Path templatePath,
                         Map<ParameterKey, ParameterValue> parameters) {
        var context = new Context();
        context.setVariable("utcZoneId", UTC);
        context.setVariable("systemDefaultZoneId", ZoneId.systemDefault());
        context.setVariable("parameters", parameters.bimap(ParameterKey::value,
                                                           ParameterValue::value)
                                                    .toJavaMap());

        return templateEngine.process(templatePath.toString(), context);
    }

    public Json<JsonNode> merge(Json<String> baseString,
                                Json<String> inputString) {
        try {
            var base = objectMapper.readTree(baseString.value());
            var input = objectMapper.readTree(inputString.value());
            var output = objectMapper.updateValue(base, input);
            return new Json<>(output);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Error merging JSON", e);
        }
    }
}
