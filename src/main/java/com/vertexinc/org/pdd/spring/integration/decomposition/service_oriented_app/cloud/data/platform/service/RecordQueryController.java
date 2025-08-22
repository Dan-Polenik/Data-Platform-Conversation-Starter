package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.query.PlatformRecordView;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.query.RecordQueryService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/org/{org}/tenant/{tenant}/topic/{topic}/query/{event}")
public class RecordQueryController {

    private final RecordQueryService service;
    private final ObjectMapper om;

    public RecordQueryController(RecordQueryService service, ObjectMapper om) {
        this.service = service;
        this.om = om;
    }

    @GetMapping
    public ResponseEntity<Object> getByCorrelationId(
            @PathVariable String org,
            @PathVariable String tenant,
            @PathVariable String topic,
            @PathVariable String event,
            @RequestParam("id") String id // the correlationId
    ) {
        return service.findByCorrelationId(id)
                .map(PlatformRecordView::from)
                .map(p -> p.payloadBase64())
                .map(s -> om.convertValue(s, Object.class))
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }
}
