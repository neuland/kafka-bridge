package de.neuland.kafkabridge.adapter.out.kafka;

import de.neuland.kafkabridge.domain.kafka.Publisher;
import de.neuland.kafkabridge.domain.kafka.RecordKey;
import de.neuland.kafkabridge.domain.kafka.RecordValue;
import de.neuland.kafkabridge.domain.kafka.Topic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.util.concurrent.CompletableFuture;

public class PublisherImpl<K, V> implements Publisher<K, V> {
    private final KafkaProducer<K, V> kafkaProducer;

    public PublisherImpl(KafkaProducer<K, V> kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
    }

    @Override
    public CompletableFuture<Void> send(Topic topic,
                                        RecordKey<K> recordKey,
                                        RecordValue<V> recordValue) {
        var future = new CompletableFuture<Void>();
        kafkaProducer.send(new ProducerRecord<>(topic.value(),
                                                recordKey.value(),
                                                recordValue.value()),
                           (metadata, exception) -> {
                               if (exception != null) {
                                   future.completeExceptionally(exception);
                               } else {
                                   future.complete(null);
                               }
                           });
        return future;
    }
}
