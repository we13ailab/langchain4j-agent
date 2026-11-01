package com.javatechie.exception;

import dev.langchain4j.exception.ToolErrorVisibleToLlm;

/**
 * Thrown when cancellation is attempted on an order that has already shipped.
 * <p>
 * Implements {@link ToolErrorVisibleToLlm} so that, when it escapes a tool,
 * the {@code ToolExecutionErrorHandler} sends only the curated
 * {@link #messageForLlm()} text to the LLM — never internal details.
 */
public class OrderCannotBeCancelledException extends RuntimeException implements ToolErrorVisibleToLlm {

    private final String orderId;

    public OrderCannotBeCancelledException(String orderId) {
        super("Cannot cancel an order that has already been shipped: " + orderId);
        this.orderId = orderId;
    }

    @Override
    public String messageForLlm() {
        return "The order with ID " + orderId
                + " has already been shipped, so it can no longer be cancelled.";
    }
}
