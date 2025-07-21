package io.labs64.apigateway.controller.audit.v1;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.labs64.audit.v1.api.AuditEventApi;
import io.labs64.audit.v1.model.AuditEvent;
import io.labs64.apigateway.service.MessagePublisherService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1")
public class AuditEventController implements AuditEventApi {

    private final MessagePublisherService messagePublisherService;

    public AuditEventController(MessagePublisherService messagePublisherService) {
        this.messagePublisherService = messagePublisherService;
    }

    @Override
    public ResponseEntity<String> publishEvent(AuditEvent event) {
        boolean res = messagePublisherService.publishMessage(event);
        if (res) {
            return ResponseEntity.ok("Message sent successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message");
        }
    }

}
