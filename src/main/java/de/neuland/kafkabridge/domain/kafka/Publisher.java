package de.neuland.kafkabridge.domain.kafka;

import java.util.concurrent.CompletableFuture;

public interface Publisher<K, V> {
    CompletableFuture<Void> send(Topic topic,
                                 RecordKey<K> recordKey,
                                 RecordValue<V> recordValue);
}
