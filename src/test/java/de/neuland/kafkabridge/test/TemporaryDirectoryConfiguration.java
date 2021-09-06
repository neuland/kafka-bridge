package de.neuland.kafkabridge.test;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.util.FileSystemUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

@TestConfiguration
public class TemporaryDirectoryConfiguration {
    private static final AtomicReference<Path> temporaryDirectoryStore = new AtomicReference<>();

    public TemporaryDirectoryConfiguration() {
        init();
    }

    public static Path getTemporaryDirectory() {
        return temporaryDirectoryStore.get();
    }

    private static void init() {
        temporaryDirectoryStore.updateAndGet(temporaryDirectory -> {

            if (temporaryDirectory == null) {
                try {
                    temporaryDirectory = Files.createTempDirectory("kafkabridge-" + TemporaryDirectoryConfiguration.class.getSimpleName());
                    registerShutdown();
                } catch (IOException e) {
                    throw new RuntimeException("Error on creating temporary directory", e);
                }
            }

            System.setProperty("kafka-bridge.template-directory", temporaryDirectory.toString());

            return temporaryDirectory;
        });
    }

    private static void registerShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            temporaryDirectoryStore.updateAndGet(temporaryDirectory -> {
                try {
                    FileSystemUtils.deleteRecursively(temporaryDirectory);
                    return null;
                } catch (IOException e) {
                    throw new RuntimeException("Error on deleting temporary directory", e);
                }
            });
        }));
    }
}
