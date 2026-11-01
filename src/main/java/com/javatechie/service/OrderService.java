package com.javatechie.service;

import com.javatechie.exception.OrderCannotBeCancelledException;
import com.javatechie.exception.OrderNotFoundException;
import com.javatechie.model.OrderEntity;
import com.javatechie.repository.OrderRepository;
import org.springframework.stereotype.Service;

@Service
public class OrderService {

    private final OrderRepository orderRepository;

    public OrderService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public OrderEntity getOrderStatus(String orderId) {

        return orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    public OrderEntity cancelOrder(String orderId) {
        OrderEntity order = orderRepository.findByOrderId(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        if (order.getStatus().equalsIgnoreCase("Shipped")) {
            throw new OrderCannotBeCancelledException(orderId);
        }
        order.setStatus("CANCELLED");
        order.setExpectedDelivery(null);
        return orderRepository.save(order); // update the order in the database
    }
}