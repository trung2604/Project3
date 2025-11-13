package com.project3.notificationservice.event;

import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.errors.RetriableException;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class EventConsumer {

    @RetryableTopic(
            attempts = "4",
            backoff = @Backoff(delay = 1000, multiplier = 2),
            autoCreateTopics = "true",
            include = {RetriableException.class, RuntimeException.class},
            dltStrategy = DltStrategy.FAIL_ON_ERROR
    )

    // Old test consumer - can be removed if not needed
    // @KafkaListener(topics = "test", containerFactory = "kafkaListenerContainerFactory")
    // public void listen(String message) {
    //     log.info("Received message : " + message);
    // }

    @DltHandler
    void processDlt(@Payload String message) {
        log.info("DLT received message : " + message);
    }
}
