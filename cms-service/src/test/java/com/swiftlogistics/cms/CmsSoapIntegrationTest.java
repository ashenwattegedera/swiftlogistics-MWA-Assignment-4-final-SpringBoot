package com.swiftlogistics.cms;

import com.swiftlogistics.common.cms.contract.CancelOrderRequest;
import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.cms.contract.OrderStatusRequest;
import com.swiftlogistics.common.cms.contract.OrderStatusResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Full SOAP/XML round-trip against the real (embedded) HTTP endpoint, exercising the
 * WSDL-driven contract, the JAXB marshaller and the MessageDispatcherServlet together.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CmsSoapIntegrationTest {

    @LocalServerPort
    private int port;

    private WebServiceTemplate template() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                CreateOrderRequest.class, CreateOrderResponse.class,
                OrderStatusRequest.class, OrderStatusResponse.class,
                CancelOrderRequest.class, CancelOrderResponse.class);
        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri("http://localhost:" + port + "/ws");
        return template;
    }

    @Test
    void createOrderThenQueryStatusThenCancel() {
        WebServiceTemplate template = template();

        CreateOrderResponse created = (CreateOrderResponse) template.marshalSendAndReceive(
                new CreateOrderRequest("C-100", "REF-42", 1250.0, "two parcels"));
        assertNotNull(created.getCmsOrderId());
        assertEquals("ACCEPTED", created.getStatus());

        OrderStatusResponse status = (OrderStatusResponse) template.marshalSendAndReceive(
                new OrderStatusRequest(created.getCmsOrderId()));
        assertEquals("ACCEPTED", status.getStatus());

        CancelOrderResponse cancelled = (CancelOrderResponse) template.marshalSendAndReceive(
                new CancelOrderRequest(created.getCmsOrderId(), "customer called"));
        assertEquals("CANCELLED", cancelled.getStatus());
    }
}
