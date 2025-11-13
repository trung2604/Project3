package com.project3.commonservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class KafkaService {
    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;
    
    @Autowired
    private ObjectMapper objectMapper;

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
        log.info("Message sent to topic {} : {}", topic, message);
    }
    
    public <T> void sendMessage(String topic, T object) {
        try {
            String jsonMessage = objectMapper.writeValueAsString(object);
            kafkaTemplate.send(topic, jsonMessage);
            log.info("Message sent to topic {} : {}", topic, jsonMessage);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize message to JSON for topic {}: {}", topic, e.getMessage(), e);
            throw new RuntimeException("Failed to send message to Kafka", e);
        }
    }
}
