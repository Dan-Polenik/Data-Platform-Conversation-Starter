package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.command;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.storage.RecordStore;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.integration.core.GenericHandler;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class AmountMetricsTap implements GenericHandler<Message<PlatformRecord<String>>> {

    private final MeterRegistry registry;
    private final RecordStore recordStore;
    private final ObjectMapper om;

    // per key metrics and mins
    private final ConcurrentMap<String, DistributionSummary> summaries = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Double> mins = new ConcurrentHashMap<>();

    public AmountMetricsTap(MeterRegistry registry, RecordStore recordStore, ObjectMapper om) {
        this.om = om;
        this.registry = registry;
        this.recordStore = recordStore;
    }

    @Override
    public Object handle(Message<PlatformRecord<String>> payload, MessageHeaders headers) {
        Map<String, String> tenantMetadata =payload.getPayload().getOperationalMetadata().get("TenantMetadata");
        // derive key
        String topic = tenantMetadata.getOrDefault("topic", "unknown");
        String event = tenantMetadata.getOrDefault("event", "unknown");
        String key = topic + "." + event;

        // extract amount from JSON string payload
        double amount;
        try {
            JsonNode root = om.readTree(payload.getPayload().getPayload());
            if (!root.hasNonNull("amount")) return null;
            amount = root.get("amount").asDouble();
        } catch (Exception e) {
            return null; // ignore bad JSON
        }

        // record into Micrometer
        DistributionSummary s = summaries.computeIfAbsent(key, k ->
                DistributionSummary.builder("trellis.amount")
                        .description("Amounts by topic.eventName")
                        .baseUnit("amount")
                        .tag("key", k)
                        .publishPercentiles(0.25, 0.50, 0.75)     // p25, p50, p75
                        .publishPercentileHistogram()
                        .register(registry)
        );
        s.record(amount);

        // track min
        mins.compute(key, (k, v) -> v == null ? amount : Math.min(v, amount));

        // snapshot and persist describe-like JSON
        io.micrometer.core.instrument.distribution.HistogramSnapshot snap = s.takeSnapshot();
        double p25 = percentile(snap, 0.25);
        double p50 = percentile(snap, 0.50);
        double p75 = percentile(snap, 0.75);

        Describe desc = new Describe(
                snap.count(),
                snap.mean(),
                mins.getOrDefault(key, Double.NaN),
                p25, p50, p75,
                snap.max()
        );

        try {
            PlatformRecord record = new PlatformRecord();
            record.setPayload(om.writeValueAsString(desc));
            recordStore.put(key, record);
        } catch (Exception ignore) {
        }

        return null; // sink
    }

    private static double percentile(io.micrometer.core.instrument.distribution.HistogramSnapshot snap, double p) {
        for (var v : snap.percentileValues()) {
            if (Math.abs(v.percentile() - p) < 1e-9) return v.value();
        }
        return Double.NaN; // not yet available
    }


    // stored shape
    public record Describe(
            long count,
            double mean,
            double min,   // our tracked min
            double p25,
            double p50,
            double p75,
            double max
    ) {
    }
}
