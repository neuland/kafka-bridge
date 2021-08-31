# Kafka Bridge

Publish Kafka messages from HTTP

## Configuration

Example configuration for commonly used user + password authentication:

```yaml
kafka-bridge:
  kafka:
    "[bootstrap.servers]": ...:9092
    "[security.protocol]": SASL_SSL
    "[sasl.mechanism]": PLAIN
    "[sasl.jaas.config]": >-
      org.apache.kafka.common.security.plain.PlainLoginModule
      required
      username="..."
      password="...";
    "[ssl.endpoint.identification.algorithm]": https
  schema-registry:
    url: https://.../
    "[basic.auth.credentials.source]": USER_INFO
    "[basic.auth.user.info]": ...:...
```

## Run

```
export SPRING_CONFIG_LOCATION=file:///.../application.yaml
java -jar kafka-bridge.jar
```

## Send Kafka message from HTTP

```
POST /topics/Products/send
Key: { "code": "Product-0001" }
Key-Content-Type: application/avro+json
Key-Schema-Subject: de.neuland.kafkabridge.ProductKey
Content-Type: application/avro+json
Schema-Subject: de.neuland.kafkabridge.Product

{
  "type": "REGULAR",
  "name": "Kafka Bridge Product",
  "available_since": 1629559054000
}
```
