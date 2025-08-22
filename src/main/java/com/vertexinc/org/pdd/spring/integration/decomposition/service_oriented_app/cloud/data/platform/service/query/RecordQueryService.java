package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.query;


import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.storage.RecordStore;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class RecordQueryService {
  private final RecordStore store;

  public RecordQueryService(RecordStore store) { this.store = store; }

  public Optional<PlatformRecord> findByCorrelationId(String id) {
    return Optional.ofNullable(store.get(id));
  }
}
