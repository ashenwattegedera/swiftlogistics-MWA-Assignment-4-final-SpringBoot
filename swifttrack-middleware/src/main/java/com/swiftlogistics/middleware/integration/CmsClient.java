package com.swiftlogistics.middleware.integration;

import com.swiftlogistics.common.cms.contract.CancelOrderRequest;
import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.cms.contract.OrderStatusRequest;
import com.swiftlogistics.common.cms.contract.OrderStatusResponse;
import org.springframework.stereotype.Component;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * SOAP client for the legacy CMS. Owns the protocol (SOAP over HTTP) and the JAXB marshalling
 * of the XML contract; the rest of the middleware only sees plain Java results.
 */
@Component
public class CmsClient {

    private final WebServiceTemplate webServiceTemplate;

    public CmsClient(WebServiceTemplate cmsWebServiceTemplate) {
        this.webServiceTemplate = cmsWebServiceTemplate;
    }

    public CreateOrderResponse createOrder(CreateOrderRequest request) {
        return (CreateOrderResponse) webServiceTemplate.marshalSendAndReceive(request);
    }

    public OrderStatusResponse getOrderStatus(String cmsOrderId) {
        return (OrderStatusResponse) webServiceTemplate.marshalSendAndReceive(
                new OrderStatusRequest(cmsOrderId));
    }

    public CancelOrderResponse cancelOrder(String cmsOrderId, String reason) {
        return (CancelOrderResponse) webServiceTemplate.marshalSendAndReceive(
                new CancelOrderRequest(cmsOrderId, reason));
    }
}
