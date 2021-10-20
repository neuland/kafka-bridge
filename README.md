# Kafka Bridge

Publish Kafka messages from HTTP

## Configuration

Example configuration for commonly used user + password authentication:

```yaml
kafka-bridge:
  kafka:
    "[bootstrap.servers]": <kafka-host>:9092
    "[security.protocol]": SASL_SSL
    "[sasl.mechanism]": PLAIN
    "[sasl.jaas.config]": >-
      org.apache.kafka.common.security.plain.PlainLoginModule
      required
      username="<kafka-username>"
      password="<kafka-password>";
    "[ssl.endpoint.identification.algorithm]": https
  schema-registry:
    url: https://<schema-registry-host>/
    "[basic.auth.credentials.source]": USER_INFO
    "[basic.auth.user.info]": <schema-registry-username>:<schema-registry-password>
```

Example configuration for setting a specific template configuration:

```yaml
kafka-bridge:
  template-directory: /kafka-bridge/templates
  template-cache-duration: 30s
```

If `template-directory` is unset, the template paths will be resolved against the working directory. 
This setting is ignored for absolute paths.

If `template-cache-duration` is unset, the template will not be cached.
No caching is the default.

## Run

```
export SPRING_CONFIG_LOCATION=file:///.../application.yaml
java -jar kafka-bridge.jar
```

## Send Kafka message from HTTP

### Example: String key + Avro value

```
POST /topics/Products/send
Key: Product-0001
Content-Type: application/avro+json
Schema-Subject: de.neuland.kafkabridge.Product

{
  "type": "REGULAR",
  "name": "Kafka Bridge Product",
  "available_since": 1629559054000
}
```

### Example: Avro key + Avro value

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

## Templating

The templating is done via Thymeleaf. In addition to what's available per default,
the [Java 8 Time Thymeleaf extras](https://github.com/thymeleaf/thymeleaf-extras-java8time) and two custom variables
have been added. The variables are `utcZoneId` and `systemDefaultZoneId` pointing to `java.time.ZoneOffset#UTC`
and `java.time.ZoneId#systemDefault` respectively.

### Example: Avro key + Avro value with templating

Contents of `key.json`:

```
{
  "code": "default"
}
```

Contents of `value.json`:

```
{
  "type": "REGULAR",
  "available_since": [(
    ${ #temporals.createNowForTimeZone(systemDefaultZoneId).minusSeconds(5).toInstant().toEpochMilli() }
  )]
}
```

Request against Kafka Bridge:

```
POST /topics/Products/send
Key: {}
Key-Content-Type: application/avro+json
Key-Schema-Subject: de.neuland.kafkabridge.ProductKey
Key-Template-Path: key.json
Content-Type: application/avro+json
Schema-Subject: de.neuland.kafkabridge.Product
Template-Path: value.json

{
  "type": "SPECIAL",
  "name": "Kafka Bridge Product"
}
```

Actual JSON key that gets converted to Avro:

```
{
  "code": "default"
}
```

Actual JSON value that gets converted to Avro:

```
{
  "type": "SPECIAL",
  "name": "Kafka Bridge Product",
  "available_since": 1630566414000
}
```

### Example: Templating with parameters

Request against Kafka Bridge:

```
POST /topics/Products/send
Key: { "code": "Product-0001" }
Key-Content-Type: application/avro+json
Key-Schema-Subject: de.neuland.kafkabridge.ProductKey
Content-Type: application/avro+json
Schema-Subject: de.neuland.kafkabridge.Product
Template-Path: value.json
Template-Parameter-name: Kafka Bridge
{
  "code": "kafka"
}
```

Contents of `value.json`:

```
{
  "name": "[( ${parameters.name} )] Product"
  "available_since": [(
    ${ #temporals.createNowForTimeZone(systemDefaultZoneId).minusSeconds(5).toInstant().toEpochMilli() }
  )]
}
```

Actual JSON value that gets converted to Avro:

```
{
  "code": "kafka",
  "name": "Kafka Bridge Product",
  "available_since": 1630566414000
}
```


Working with defaults:

```
[# th:with="type=${parameters.type} ?: 'REGULAR'" ]
{
  "type": "[( ${type} )]",
  "available_since": [(
    ${ #temporals.createDateTime("2021-08-31T20:30:00").minusSeconds(5).atZone(utcZoneId).toInstant().toEpochMilli() }
  )]
}
[/]
```
