package io.labs64.apigateway.controller.checkout.v1;

import io.labs64.apigateway.service.ShoppingCartPublisherService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.labs64.checkout.v1.api.CheckoutApi;
import io.labs64.checkout.v1.model.ShoppingCart;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class CheckoutController implements CheckoutApi {

    private static final Logger logger = LoggerFactory.getLogger(CheckoutController.class);

    private final ShoppingCartPublisherService shoppingCartPublisherService;

    public CheckoutController(ShoppingCartPublisherService shoppingCartPublisherService) {
        this.shoppingCartPublisherService = shoppingCartPublisherService;
    }

    @Override
    public ResponseEntity<String> initiateCheckout(ShoppingCart cart) {
        boolean res = shoppingCartPublisherService.publishShoppingCart(cart);
        if (res) {
            return ResponseEntity.ok("Message sent successfully");
        } else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to send message");
        }
    }

    @Override
    public ResponseEntity<ShoppingCart> getCartById(UUID cartId) {
        logger.debug("Received request to get cart by ID: {}", cartId);
        return CheckoutApi.super.getCartById(cartId);
    }

}
