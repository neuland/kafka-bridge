package de.neuland.kafkabridge.domain.kafka;

import io.vavr.concurrent.Future;

public interface Publisher<K, V> {
    Future<Void> send(Topic topic,
                      RecordKey<K> recordKey,
                      RecordValue<V> recordValue);
}