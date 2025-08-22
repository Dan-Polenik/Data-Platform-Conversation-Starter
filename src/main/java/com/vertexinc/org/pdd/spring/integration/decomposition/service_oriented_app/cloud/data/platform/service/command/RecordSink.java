package com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.command;

import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.model.PlatformRecord;
import com.vertexinc.org.pdd.spring.integration.decomposition.service_oriented_app.cloud.data.platform.service.storage.RecordStore;
import org.springframework.integration.core.GenericHandler;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHeaders;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

import static org.springframework.messaging.MessageHeaders.ID;

@Component
public class RecordSink implements GenericHandler<PlatformRecord> {
    private final RecordStore store;

    public RecordSink(RecordStore store) {
        this.store = store;
    }

    @Override
    public Object handle(PlatformRecord payload, MessageHeaders headers) {
        payload.getTransactionalMetadata().put("messageHeaders", headers);
        String cid = ((UUID) headers.get(ID)).toString().trim();
        store.put(cid, payload);
        return payload;
    }


}
