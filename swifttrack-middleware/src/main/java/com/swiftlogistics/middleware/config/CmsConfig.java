package com.swiftlogistics.middleware.config;

import com.swiftlogistics.common.cms.contract.CancelOrderRequest;
import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.cms.contract.OrderStatusRequest;
import com.swiftlogistics.common.cms.contract.OrderStatusResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.client.core.WebServiceTemplate;

/**
 * The SOAP client template for the legacy CMS, exposed as a bean so tests can bind a
 * {@code MockWebServiceServer} to it.
 */
@Configuration
public class CmsConfig {

    @Bean
    public WebServiceTemplate cmsWebServiceTemplate(
            @Value("${swifttrack.cms.url:http://localhost:8081/ws}") String url) {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                CreateOrderRequest.class, CreateOrderResponse.class,
                OrderStatusRequest.class, OrderStatusResponse.class,
                CancelOrderRequest.class, CancelOrderResponse.class);
        WebServiceTemplate template = new WebServiceTemplate(marshaller);
        template.setDefaultUri(url);
        return template;
    }
}
