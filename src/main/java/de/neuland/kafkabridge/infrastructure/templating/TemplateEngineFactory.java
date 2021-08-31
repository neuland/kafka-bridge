package de.neuland.kafkabridge.infrastructure.templating;

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
    public ITemplateEngine templateEngine() {
        var templateEngine = new TemplateEngine();
        templateEngine.addDialect(new Java8TimeDialect());
        templateEngine.setTemplateResolver(templateResolver());
        return templateEngine;
    }

    private ITemplateResolver templateResolver() {
        var fileTemplateResolver = new FileTemplateResolver();
//        fileTemplateResolver.setPrefix("");
        fileTemplateResolver.setCacheTTLMs(3600000L);
        fileTemplateResolver.setCacheable(true);
        return fileTemplateResolver;
    }
}
