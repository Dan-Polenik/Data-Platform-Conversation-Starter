# Cloud Data Platform POC

A tiny Spring Boot and Spring Integration app that turns HTTP events into `PlatformRecord`s, stores them, and keeps simple running metrics per `topic.event`. Metrics are backed by Micrometer and saved to the same record store so you can query them like any other record.

## What it does

* HTTP POST turns a JSON payload into a `PlatformRecord`
* Headers are captured into `transactionalMetadata`
* `RecordSink` writes the record to a `RecordStore`
* A wire tap (`AmountMetricsTap`) watches the stream, extracts `amount` from the JSON string, updates Micrometer stats, and persists a pandas describe style snapshot keyed by `topic.event`
* You can

  * fetch the original payload by id
  * fetch the rolling metrics by topic and event

## Quick start

**Prereqs**

* JDK 17
* Gradle
* Port 8080 free

**Run**

```bash
./gradlew bootRun
# or build a jar
./gradlew clean build
java -jar build/libs/service-oriented-app-0.0.1-SNAPSHOT.jar
```

## API

### Ingest an event

`POST /org/{org}/tenant/{tenant}/topic/{topic}/event/{event}`

* Body is JSON. It is stored as a string in the `payload` field of the `PlatformRecord`.
* If your JSON includes `"amount": <number or string>`, the metrics tap will update stats for this `topic.event`.

**Example**

```bash
http POST \
  http://localhost:8080/org/myOrg/tenant/myTenant/topic/myTopic/event/myEvent \
  Content-Type:application/json \
  amount=34
```

**Sample response**

```json
{
  "businessMetadata": null,
  "operationalMetadata": {
    "TenantMetadata": {
      "event": "myEvent",
      "org": "myOrg",
      "tenant": "myTenant",
      "topic": "myTopic"
    }
  },
  "payload": "{\"amount\": \"34\"}",
  "transactionalMetadata": { "...": "HTTP and message headers" }
}
```

### Get the original payload by id

`GET /org/{org}/tenant/{tenant}/topic/{topic}/query/{event}?id={messageId}`

**Example**

```bash
http GET \
  "http://localhost:8080/org/myOrg/tenant/myTenant/topic/myTopic/query/myEvent" \
  id==cbb19d6e-29f4-e21d-cb90-93b03ee6af0f
```

**Sample response**

```json
{ "amount": "34" }
```

### Get rolling metrics for a topic and event

`GET /metrics/topic/{topic}/event/{event}`

Returns a snapshot saved by the wire tap. Shape mirrors a simple pandas describe.

**Sample response**

```json
{
  "count": 42,
  "mean": 100.5,
  "std": 12.3,
  "min": 80.0,
  "p25": 95.0,
  "p50": 100.0,
  "p75": 105.0,
  "max": 120.0
}
```

## How it works

* **`IntegrationConfig`**
  Defines an `Http.inboundGateway` at `/org/{org}/tenant/{tenant}/topic/{topic}/event/{event}`. Converts the body into a `PlatformRecord` and sends it to the `records` channel.
* **`RecordSink`**
  Consumes from `records` and writes each record to the `RecordStore`.
* **`AmountMetricsTap`**
  Wire tap on the same flow. Parses the JSON string payload, extracts `amount`, updates a Micrometer `DistributionSummary` tagged by `topic.event`, tracks a running min, builds a describe style snapshot, and upserts it in the `RecordStore` with key `topic.event`.
* **`RecordQueryController`**
  Reads original payloads by id.
* **`MetricsController`**
  Reads the latest describe snapshot for a `topic.event`.

## Project layout

```
src/main/java/com/vertexinc/.../cloud/data/platform
  config/
    IntegrationConfig.java
  model/
    PlatformRecord.java
  service/
    command/
      AmountMetricsTap.java
      RecordSink.java
    query/
      PlatformRecordView.java
      RecordQueryService.java
    storage/
      RecordStore.java
  MetricsController.java
  RecordQueryController.java
  ServiceOrientedAppApplication.java
```

## Build setup

Gradle snippets used in this POC:

```gradle
plugins {
  id "java"
  id "org.springframework.boot" version "3.5.4"
  id "io.spring.dependency-management" version "1.1.7"
}

java { toolchain { languageVersion = JavaLanguageVersion.of(17) } }

dependencies {
  compileOnly "org.projectlombok:lombok:1.18.34"
  annotationProcessor "org.projectlombok:lombok:1.18.34"

  implementation "org.springframework.boot:spring-boot-starter-web"
  implementation "org.springframework.boot:spring-boot-starter-integration"
  implementation "org.springframework.integration:spring-integration-http"
  implementation "org.springframework.boot:spring-boot-starter-actuator"
  implementation "io.micrometer:micrometer-core"

  testImplementation "org.springframework.boot:spring-boot-starter-test"
  testImplementation "io.projectreactor:reactor-test"
  testImplementation "org.springframework.integration:spring-integration-test"
  testRuntimeOnly "org.junit.platform:junit-platform-launcher"
}
```

Optional registries:

```gradle
implementation "io.micrometer:micrometer-registry-prometheus"
// or graphite, influx, datadog, statsd
```

If you add Prometheus, Spring Boot will expose `/actuator/prometheus`.

## Configuration

No required config for the demo. Defaults are in code. If you wire a real metrics backend, use normal Spring Boot Micrometer properties.

## Swap the store

`RecordStore` is an interface. The sample can be in memory. To persist, add an implementation for Postgres, Redis, or S3 and inject it.

## Notes and tips

* The metrics tap expects an `amount` field in the JSON. Number or numeric string works.
* Keys for metrics are `topic.event`. This keeps stats hot per event name inside a topic.
* Everything runs in process, so it is easy to demo locally.

## 1 percent improvements

* Add an endpoint to list all keys that have metrics, useful for dashboards
* Guard against absurd values with a small validator before recording
* Expose a simple health metric counting records per key so you can spot hot partitions fast
