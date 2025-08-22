package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;
@Getter
@Setter
public class PlatformRecord<T> {
    private T payload;
    private Map<String, ? extends Map> businessMetadata;
    private Map<String, ? extends Map> operationalMetadata;
    private Map<String, ? extends Map> transactionalMetadata;

}