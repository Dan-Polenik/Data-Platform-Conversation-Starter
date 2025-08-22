package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.storage;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RecordStore {
    private final ConcurrentHashMap<String, PlatformRecord> records = new ConcurrentHashMap<>();
    public void put(String correlationId, PlatformRecord record) { records.put(correlationId, record); }
    public PlatformRecord get(String correlationId) { return records.get(correlationId); }
    public Map<String, PlatformRecord> view() { return records; }
}
