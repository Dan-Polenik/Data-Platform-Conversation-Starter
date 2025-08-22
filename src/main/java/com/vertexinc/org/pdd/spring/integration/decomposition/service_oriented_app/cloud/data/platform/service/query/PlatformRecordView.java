package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.query;


import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;

import java.util.Base64;
import java.util.Map;

public record PlatformRecordView(
    String payloadBase64,
    Map<String,?> businessMetadata,
    Map<String,?> operationalMetadata,
    Map<String,?> transactionalMetadata
) {
  public static PlatformRecordView from(PlatformRecord<String> r) {
    return new PlatformRecordView(
        r.getPayload() != null ? r.getPayload() : null,
        r.getBusinessMetadata(),
        r.getOperationalMetadata(),
        r.getTransactionalMetadata()
    );
  }
}
