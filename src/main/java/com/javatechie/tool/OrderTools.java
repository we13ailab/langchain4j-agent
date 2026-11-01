package com.javatechie.tool;

import com.javatechie.model.OrderEntity;
import com.javatechie.service.OrderService;
import dev.langchain4j.agent.tool.Tool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class OrderTools {

    private static final Logger log = LoggerFactory.getLogger(OrderTools.class);

    private final OrderService orderService;

    public OrderTools(OrderService orderService) {
        this.orderService = orderService;
    }

    @Tool("Get the current status and delivery information for an order")
    public OrderEntity getOrderStatus(String orderId) {
        log.info("TOOL CALLED -> getOrderStatus({})", orderId);
        return orderService.getOrderStatus(orderId);
    }

    @Tool("Cancel an order")
    public OrderEntity cancelOrder(String orderId) {
        log.info("TOOL CALLED -> cancelOrder({})", orderId);
        return orderService.cancelOrder(orderId);
    }
}
