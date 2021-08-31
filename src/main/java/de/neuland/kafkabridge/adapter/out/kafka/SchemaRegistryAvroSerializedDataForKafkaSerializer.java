package de.neuland.kafkabridge.adapter.out.kafka;

import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import org.apache.kafka.common.serialization.Serializer;
import org.springframework.stereotype.Component;

@Component
public class SchemaRegistryAvroSerializedDataForKafkaSerializer implements Serializer<SchemaRegistryAvroSerializedDataForKafka> {
    @Override
    public byte[] serialize(String topic,
                            SchemaRegistryAvroSerializedDataForKafka data) {
        return data.data();
    }
}
