package com.swiftlogistics.cms.endpoint;

import com.swiftlogistics.cms.domain.CmsOrder;
import com.swiftlogistics.cms.service.CmsOrderService;
import com.swiftlogistics.common.cms.contract.CancelOrderRequest;
import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.cms.contract.OrderStatusRequest;
import com.swiftlogistics.common.cms.contract.OrderStatusResponse;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class CmsOrderEndpoint {

    private static final String NAMESPACE_URI = "http://www.swiftlogistics.lk/cms";

    private final CmsOrderService orderService;

    public CmsOrderEndpoint(CmsOrderService orderService) {
        this.orderService = orderService;
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "createOrderRequest")
    @ResponsePayload
    public CreateOrderResponse createOrder(@RequestPayload CreateOrderRequest request) {
        CmsOrder order = orderService.createOrder(
                request.getClientId(),
                request.getClientReference(),
                request.getAmount(),
                request.getNotes());
        return new CreateOrderResponse(order.getCmsOrderId(), order.getStatus(),
                "Order accepted by CMS");
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "orderStatusRequest")
    @ResponsePayload
    public OrderStatusResponse getOrderStatus(@RequestPayload OrderStatusRequest request) {
        return orderService.find(request.getCmsOrderId())
                .map(o -> new OrderStatusResponse(o.getCmsOrderId(), o.getStatus(), "Found"))
                .orElse(new OrderStatusResponse(request.getCmsOrderId(), "NOT_FOUND",
                        "No order with id " + request.getCmsOrderId()));
    }

    @PayloadRoot(namespace = NAMESPACE_URI, localPart = "cancelOrderRequest")
    @ResponsePayload
    public CancelOrderResponse cancelOrder(@RequestPayload CancelOrderRequest request) {
        return orderService.cancel(request.getCmsOrderId(), request.getReason())
                .map(o -> new CancelOrderResponse(o.getCmsOrderId(), o.getStatus(), "Cancelled"))
                .orElse(new CancelOrderResponse(request.getCmsOrderId(), "NOT_FOUND",
                        "No order with id " + request.getCmsOrderId()));
    }
}
