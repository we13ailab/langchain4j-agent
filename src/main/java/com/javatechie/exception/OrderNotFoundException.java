package com.javatechie.exception;

import dev.langchain4j.exception.ToolErrorVisibleToLlm;

/**
 * Thrown when an order ID does not exist.
 * <p>
 * Implements {@link ToolErrorVisibleToLlm} so that, when it escapes a tool,
 * the {@code ToolExecutionErrorHandler} sends only the curated
 * {@link #messageForLlm()} text to the LLM — never internal details.
 */
public class OrderNotFoundException extends RuntimeException implements ToolErrorVisibleToLlm {

    private final String orderId;

    public OrderNotFoundException(String orderId) {
        super("Order not found: " + orderId);
        this.orderId = orderId;
    }

    @Override
    public String messageForLlm() {
        return "There is no order with ID " + orderId
                + ". Ask the user to double-check the order number.";
    }
}
