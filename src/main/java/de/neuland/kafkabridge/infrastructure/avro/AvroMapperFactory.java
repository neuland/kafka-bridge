package de.neuland.kafkabridge.infrastructure.avro;

import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AvroMapperFactory {
    @Bean
    public AvroMapper avroMapper() {
        return new AvroMapper();
    }
}
