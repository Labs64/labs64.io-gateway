package io.labs64.apigateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.labs64.audit.api.AuditEventPublisherApi;
import io.labs64.audit.model.AuditEvent;
import io.labs64.apigateway.service.MessagePublisherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuditEventPublisherController implements AuditEventPublisherApi {

    private static final Logger logger = LoggerFactory.getLogger(AuditEventPublisherController.class);

    private final MessagePublisherService messagePublisherService;
    private final ObjectMapper objectMapper;

    public AuditEventPublisherController(MessagePublisherService messagePublisherService, ObjectMapper objectMapper) {
        this.messagePublisherService = messagePublisherService;
        this.objectMapper = objectMapper;
    }

    @Override
    public ResponseEntity<String> publishEvent(AuditEvent event) {
        String eventJson;
        try {
            eventJson = objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert AuditEvent to JSON! Error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to convert AuditEvent to JSON. Error: " + e.getMessage());
        }

        boolean res = messagePublisherService.publishMessage(eventJson);
        if (res) {
            return ResponseEntity.ok("Message sent successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message");
        }
    }

}
