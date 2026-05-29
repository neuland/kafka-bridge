package de.neuland.kafkabridge.infrastructure.templating;

import de.neuland.kafkabridge.infrastructure.configuration.KafkaBridgeConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.extras.java8time.dialect.Java8TimeDialect;
import org.thymeleaf.templateresolver.FileTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

import java.io.File;


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
        kafkaBridgeConfiguration.getMaybeTemplateDirectory().ifPresent(templateDirectory ->
            fileTemplateResolver.setPrefix(templateDirectory.toString() + File.separator));
        var maybeCacheDuration = kafkaBridgeConfiguration.getMaybeTemplateCacheDuration();
        if (maybeCacheDuration.isPresent()) {
            fileTemplateResolver.setCacheTTLMs(maybeCacheDuration.get().toMillis());
            fileTemplateResolver.setCacheable(true);
        } else {
            fileTemplateResolver.setCacheable(false);
        }
        return fileTemplateResolver;
    }
}
