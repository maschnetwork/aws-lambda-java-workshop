package com.unicorn.store.data;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.unicorn.store.exceptions.PublisherException;
import com.unicorn.store.model.Unicorn;
import com.unicorn.store.model.UnicornEventType;
import com.unicorn.store.otel.TracingRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
// >= Dockerfile_04
// import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.services.eventbridge.EventBridgeAsyncClient;
import software.amazon.awssdk.services.eventbridge.model.EventBridgeException;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequest;
import software.amazon.awssdk.services.eventbridge.model.PutEventsRequestEntry;

import java.util.concurrent.ExecutionException;

@Service
public class UnicornPublisher {

    private final ObjectMapper objectMapper;

    private Logger logger = LoggerFactory.getLogger(TracingRequestInterceptor.class);

    // Dockerfile_00_ ... 03
    private static final EventBridgeAsyncClient eventBridgeClient = EventBridgeAsyncClient
            .builder()
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build();
    // >= Dockerfile_04
    // @Autowired
    // private EventBridgeAsyncClient eventBridgeClient;

    public UnicornPublisher(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void publish(Unicorn unicorn, UnicornEventType unicornEventType) {
        try {
            var unicornJson = objectMapper.writeValueAsString(unicorn);
            var eventsRequest = createEventRequestEntry(unicornEventType, unicornJson);

            eventBridgeClient.putEvents(eventsRequest).get();
            logger.info("Publishing ...");
            logger.info(unicornJson);
        } catch (JsonProcessingException e) {
            logger.error("Error JsonProcessingException ...");
            logger.error(e.getMessage());
        } catch (EventBridgeException | ExecutionException | InterruptedException e) {
            logger.error("Error EventBridgeException | ExecutionException ...");
            logger.error(e.getMessage());
        }
    }

    private PutEventsRequest createEventRequestEntry(UnicornEventType unicornEventType, String unicornJson) {
        var entry = PutEventsRequestEntry.builder()
                .source("com.unicorn.store")
                .eventBusName("unicorns")
                .detailType(unicornEventType.name())
                .detail(unicornJson)
                .build();

        return PutEventsRequest.builder()
                .entries(entry)
                .build();
    }
}
