# Component architecture

This service is an event-oriented HTTP to channel bridge. We care about the **record**, not its inner shape. The payload is treated as an opaque JSON string. Everything else is metadata and routing.

## Goals

1. Accept HTTP events and turn them into `PlatformRecord`s
2. Publish each record to a Spring Integration pub sub channel
3. Let independent subscribers do work without coupling to payload structure
4. Keep a simple read model for demos and local exploration

## Big picture

* MVC Controller receives POST
* Record is enriched into a `PlatformRecord`
* Record is published to a pub sub channel
* Subscribers:

  1. Save the full record in the record store
  2. Wire tap extracts `amount` and updates rolling metrics, then stores a describe style snapshot
* Read only controllers fetch either the original payload by id or the metrics snapshot by `topic.event`

Diagram: `Data-Platform.Components.png`

## Components

**MVC Controller (ingest)**
HTTP entry point at
`/org/{org}/tenant/{tenant}/topic/{topic}/event/{event}`
Builds a `PlatformRecord` with:

* `payload` as the original JSON string
* `transactionalMetadata` from HTTP headers and message headers
* `operationalMetadata.TenantMetadata` from the path parts

**Spring Integration channel**
Publish subscribe channel that fans out records. Writers and readers do not block each other.

**Enrich to PlatformRecord**
Stateless step that wraps the request into the record shape described above.

**Record sink**
Subscriber that writes the full record to `RecordStore`. Keyed by message id.

**Amount metrics tap**
Subscriber that parses the JSON string only to read `amount`. Records it in Micrometer and persists a pandas describe style snapshot to `RecordStore` keyed by `topic.event`.

**Read only data service**
Thin service used by query controllers. No writes. Looks up by id or by `topic.event`.

**Query controllers**

* Metrics: `GET /metrics/topic/{topic}/event/{event}`
* Payload by id: `GET /org/{org}/tenant/{tenant}/topic/{topic}/query/{event}?id={messageId}`

## Record first, content second

* The **record** is the stable unit. It carries payload and context.
* Subscribers decide how much of the payload to understand. In this demo only the metrics tap reads one field, `amount`.
* Everything else should treat `payload` as opaque. That keeps the flow honest and easy to extend.

## Request and response examples

**Ingest**

```
POST /org/myOrg/tenant/myTenant/topic/myTopic/event/myEvent
Body: { "amount": 34 }
```

**Query payload**

```
GET  /org/myOrg/tenant/myTenant/topic/myTopic/query/myEvent?id=<messageId>
```

**Query metrics**

```
GET  /metrics/topic/myTopic/event/myEvent
```

## Swapping parts

* `RecordStore` is an interface. Replace the in memory demo with Postgres, Redis, S3, or anything that can store bytes by key.
* Add more subscribers to the pub sub channel for transforms, routing, or forwarding to a broker.
* Replace the metrics tap if you want different features or different fields.

## Notes for engineers

* Event orientation means you can add subscribers without touching the ingest controller.
* Treat headers and metadata as first class data for routing and observability.
* Keep writes inside the integration flow. Keep reads inside controllers. This separation makes local demos feel like production flows.

## 1 percent improvements

1. Add an endpoint that lists all `topic.event` keys that have metrics snapshots
2. Add a small validator for absurd `amount` values before recording metrics
3. Make the JSON path for `amount` configurable for future events that nest the value
