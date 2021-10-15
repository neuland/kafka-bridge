package de.neuland.kafkabridge.infrastructure.templating;

import de.neuland.kafkabridge.infrastructure.configuration.KafkaBridgeConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;


@Configuration
public class TemplateEngineFactory {
    @Bean
    public ITemplateEngine templateEngine(KafkaBridgeConfiguration kafkaBridgeConfiguration) {
        var templateEngine = new TemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.setTemplateResolver(templateResolver(kafkaBridgeConfiguration));
        return templateEngine;
    }

    private ITemplateResolver templateResolver(KafkaBridgeConfiguration kafkaBridgeConfiguration) {
        var fileTemplateResolver = new FileTemplateResolver();
        kafkaBridgeConfiguration.getMaybeTemplateCacheDuration().peek(cacheDuration -> {
            fileTemplateResolver.setCacheTTLMs(cacheDuration.toMillis());
            fileTemplateResolver.setCacheable(true);
        }).onEmpty(() -> {
            fileTemplateResolver.setCacheable(false);
        });
        return fileTemplateResolver;
    }
}
