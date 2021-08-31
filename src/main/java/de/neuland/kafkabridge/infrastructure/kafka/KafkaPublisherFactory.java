package de.neuland.kafkabridge.infrastructure.kafka;

import de.neuland.kafkabridge.adapter.out.kafka.PublisherImpl;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import de.neuland.kafkabridge.domain.kafka.Publisher;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaPublisherFactory {
    @Bean
    public Publisher<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValueKafkaPublisher(KafkaProducer<String, SchemaRegistryAvroSerializedDataForKafka> kafkaProducer) {
        return new PublisherImpl<>(kafkaProducer);
    }

    @Bean
    public Publisher<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValueKafkaPublisher(KafkaProducer<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> kafkaProducer) {
        return new PublisherImpl<>(kafkaProducer);
    }
}
