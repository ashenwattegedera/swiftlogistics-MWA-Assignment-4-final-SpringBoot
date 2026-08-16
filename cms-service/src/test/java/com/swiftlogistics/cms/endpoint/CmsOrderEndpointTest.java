package com.swiftlogistics.cms.endpoint;

import com.swiftlogistics.cms.service.CmsOrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.webservices.server.WebServiceServerTest;
import org.springframework.context.annotation.Import;
import org.springframework.ws.test.server.MockWebServiceClient;
import org.springframework.ws.test.server.RequestCreators;
import org.springframework.ws.test.server.ResponseMatchers;
import org.springframework.xml.transform.StringSource;

@WebServiceServerTest
@Import(CmsOrderService.class)
class CmsOrderEndpointTest {

    @Autowired
    private MockWebServiceClient client;

    @Test
    void createOrderReturnsAccepted() {
        StringSource request = new StringSource(
                "<ns:createOrderRequest xmlns:ns='http://www.swiftlogistics.lk/cms'>" +
                        "<ns:clientId>C-100</ns:clientId>" +
                        "<ns:clientReference>REF-1</ns:clientReference>" +
                        "<ns:amount>250.0</ns:amount>" +
                        "</ns:createOrderRequest>");

        StringSource response = new StringSource(
                "<ns:createOrderResponse xmlns:ns='http://www.swiftlogistics.lk/cms'>" +
                        "<ns:cmsOrderId>CMS-1001</ns:cmsOrderId>" +
                        "<ns:status>ACCEPTED</ns:status>" +
                        "<ns:message>Order accepted by CMS</ns:message>" +
                        "</ns:createOrderResponse>");

        client.sendRequest(RequestCreators.withPayload(request))
                .andExpect(ResponseMatchers.payload(response));
    }

    @Test
    void getOrderStatusReturnsNotFoundForUnknownId() {
        StringSource request = new StringSource(
                "<ns:orderStatusRequest xmlns:ns='http://www.swiftlogistics.lk/cms'>" +
                        "<ns:cmsOrderId>CMS-9999</ns:cmsOrderId>" +
                        "</ns:orderStatusRequest>");

        StringSource response = new StringSource(
                "<ns:orderStatusResponse xmlns:ns='http://www.swiftlogistics.lk/cms'>" +
                        "<ns:cmsOrderId>CMS-9999</ns:cmsOrderId>" +
                        "<ns:status>NOT_FOUND</ns:status>" +
                        "<ns:message>No order with id CMS-9999</ns:message>" +
                        "</ns:orderStatusResponse>");

        client.sendRequest(RequestCreators.withPayload(request))
                .andExpect(ResponseMatchers.payload(response));
    }
}
