package de.neuland.kafkabridge.adapter.out.kafka;

import de.neuland.kafkabridge.domain.kafka.Publisher;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import io.vavr.concurrent.Future;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

public class PublisherImpl<K, V> implements Publisher<K, V> {
    private final KafkaProducer<K, V> kafkaProducer;

    public PublisherImpl(KafkaProducer<K, V> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public Future<Void> send(Topic topic,
                             RecordKey<K> recordKey,
                             RecordValue<V> recordValue) {
        return Future.fromJavaFuture(kafkaProducer.send(new ProducerRecord<>(topic.value(),
                                                                             recordKey.value(),
                                                                             recordValue.value())))
                     .map(recordMetadata -> null);
    }
}
