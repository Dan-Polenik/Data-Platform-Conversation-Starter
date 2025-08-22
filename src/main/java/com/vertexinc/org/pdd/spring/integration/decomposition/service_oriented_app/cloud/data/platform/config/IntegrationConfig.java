package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.config;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.command.AmountMetricsTap;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.command.RecordSink;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.RequestEntity;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.http.dsl.Http;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.web.util.UriTemplate;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class IntegrationConfig {

    @Bean
    public IntegrationFlow httpIngestFlow( MessageChannel records) {
        return IntegrationFlow
                .from(Http.inboundGateway("/org/{org}/tenant/{tenant}/topic/{topic}/event/{event}")
                        .requestMapping(m -> m.methods(HttpMethod.POST))
                        .payloadFunction(this::toPlatformRecord).requestPayloadType(String.class)
                        )
                .bridge().channel(records)
                .get();
    }

    public PlatformRecord<String> toPlatformRecord(HttpEntity<String> entity) {
        PlatformRecord<String> r = new PlatformRecord<>();
        String s = entity.getBody();
        Map<String, String> headers = entity.getHeaders().toSingleValueMap();
        Map<String, Map<?,?>> transactional = new LinkedHashMap<>();
        Map<String, Map<?,?>> operational = new LinkedHashMap<>();
        operational.put("TenantMetadata", extractPathVariables(((RequestEntity)entity).getUrl().getPath()));
        transactional.put("HttpHeaders", headers);
        r.setPayload(entity.getBody());
        r.setOperationalMetadata(operational);
        r.setTransactionalMetadata(transactional);
        return r;
    }
    public Map<String, String> extractPathVariables(String uri) {
        String pattern = "/org/{org}/tenant/{tenant}/topic/{topic}/event/{event}";
        UriTemplate template = new UriTemplate(pattern);
        return template.match(uri);
    }
    // Channel to collect all records for indexing
    @Bean
    public MessageChannel replyChannel() { return new org.springframework.integration.channel.DirectChannel(); }


    // Channel to collect all records for indexing
    @Bean
    public MessageChannel records() { return new org.springframework.integration.channel.DirectChannel(); }

    // Subscriber that indexes records by correlationId
    @Bean
    public IntegrationFlow indexerFlow(RecordSink sink, AmountMetricsTap metricsHandler) {
            return IntegrationFlow.from("records")
                    .transform(sink)
                    .wireTap(flow -> flow
                .channel("amountMetrics")
                .handle(metricsHandler))
                .get();
    }

}
