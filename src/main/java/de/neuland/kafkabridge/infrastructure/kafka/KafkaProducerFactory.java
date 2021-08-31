package de.neuland.kafkabridge.infrastructure.kafka;

import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import de.neuland.kafkabridge.infrastructure.configuration.KafkaBridgeConfiguration;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.common.serialization.Serializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class KafkaProducerFactory {
    @Bean
    public KafkaProducer<String, SchemaRegistryAvroSerializedDataForKafka> stringKeyAvroValueKafkaProducer(KafkaBridgeConfiguration kafkaBridgeConfiguration,
                                                                                                           Serializer<String> keySerializer,
                                                                                                           Serializer<SchemaRegistryAvroSerializedDataForKafka> valueSerializer) {
        return new KafkaProducer<>(kafkaBridgeConfiguration.getKafka(),
                                   keySerializer,
                                   valueSerializer);
    }

    @Bean
    public KafkaProducer<SchemaRegistryAvroSerializedDataForKafka, SchemaRegistryAvroSerializedDataForKafka> avroKeyAvroValueKafkaProducer(KafkaBridgeConfiguration kafkaBridgeConfiguration,
                                                                                                                                           Serializer<SchemaRegistryAvroSerializedDataForKafka> serializer) {
        return new KafkaProducer<>(kafkaBridgeConfiguration.getKafka(),
                                   serializer,
                                   serializer);
    }

    @Bean
    public Serializer<String> stringSerializer() {
        return new StringSerializer();
    }
}
