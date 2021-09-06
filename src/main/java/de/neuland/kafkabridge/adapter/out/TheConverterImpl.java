package de.neuland.kafkabridge.adapter.out;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.avro.AvroMapper;
import de.neuland.kafkabridge.domain.Json;
import de.neuland.kafkabridge.domain.SchemaRegistryAvroSerializedDataForKafka;
import de.neuland.kafkabridge.domain.TheConverter;
import de.neuland.kafkabridge.domain.schemaregistry.AvroSchema;
import de.neuland.kafkabridge.domain.schemaregistry.SchemaId;
import io.vavr.control.Try;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;

@Component
public class TheConverterImpl implements TheConverter<JsonNode> {
    private static final byte MAGIC_BYTE = 0x0;
    private static final int MAGIC_BYTE_SIZE = Byte.BYTES;
    private static final int ID_SIZE = Integer.BYTES;
    private static final int HEADER_SIZE = MAGIC_BYTE_SIZE + ID_SIZE;

    private final AvroMapper avroMapper;

    public TheConverterImpl(AvroMapper avroMapper) {
        this.avroMapper = avroMapper;
    }

    @Override
    public Try<SchemaRegistryAvroSerializedDataForKafka> convert(Json<JsonNode> json,
                                                                 AvroSchema avroSchema) {
        var schemaWriter = avroMapper.writer(new com.fasterxml.jackson.dataformat.avro.AvroSchema(avroSchema.rawSchema()));

        return Try.of(json::value)
                  .mapTry(schemaWriter::writeValueAsBytes)
                  .map(bytes -> prependAvroSchemaHeader(avroSchema.schemaId(), bytes))
                  .map(SchemaRegistryAvroSerializedDataForKafka::new);
    }

    private byte[] prependAvroSchemaHeader(SchemaId schemaId,
                                           byte[] bytes) {
        var result = new byte[HEADER_SIZE + bytes.length];
        result[0] = MAGIC_BYTE;
        System.arraycopy(ByteBuffer.allocate(ID_SIZE).putInt(schemaId.value()).array(),
                         0,
                         result,
                         MAGIC_BYTE_SIZE,
                         ID_SIZE);
        System.arraycopy(bytes,
                         0,
                         result,
                         HEADER_SIZE,
                         bytes.length);
        return result;
    }
}
