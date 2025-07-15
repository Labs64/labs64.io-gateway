package io.labs64.apigateway.controller.checkout.v1;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.labs64.checkout.v1.api.CheckoutApi;
import io.labs64.checkout.v1.model.ShoppingCart;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

@RestController
@RequestMapping("/api/v1")
public class CheckoutController implements CheckoutApi {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private final ObjectMapper objectMapper;

    public CheckoutController(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }
    @Override
    public ResponseEntity<io.labs64.checkout.v1.model.InitiateCheckout200Response> initiateCheckout(ShoppingCart cart) {
        String cartJson;
        try {
            cartJson = objectMapper.writeValueAsString(cart);
        } catch (JsonProcessingException e) {
            logger.error("Failed to convert ShoppingCart to JSON! Error: {}", e.getMessage());
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        logger.debug("Cart object received: {}", cartJson);
        return new ResponseEntity<>(HttpStatus.NOT_IMPLEMENTED);
    }

}
