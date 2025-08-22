package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.storage.RecordStore;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

  private final RecordStore recordStore;
  private final ObjectMapper om = new ObjectMapper();

  public MetricsController(RecordStore recordStore) {
    this.recordStore = recordStore;
  }

  @GetMapping("/topic/{topic}/event/{event}")
  public ResponseEntity<?> getMetrics(
      @PathVariable String topic,
      @PathVariable String event) {
    String key = topic + "." + event;

    return ResponseEntity.ok(recordStore.get(key).getPayload());


  }
}
