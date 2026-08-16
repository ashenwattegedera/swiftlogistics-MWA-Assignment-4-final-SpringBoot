package com.swiftlogistics.cms.config;

import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import com.swiftlogistics.common.cms.contract.CancelOrderRequest;
import com.swiftlogistics.common.cms.contract.CancelOrderResponse;
import com.swiftlogistics.common.cms.contract.CreateOrderRequest;
import com.swiftlogistics.common.cms.contract.CreateOrderResponse;
import com.swiftlogistics.common.cms.contract.OrderStatusRequest;
import com.swiftlogistics.common.cms.contract.OrderStatusResponse;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;
import org.springframework.ws.transport.http.MessageDispatcherServlet;
import org.springframework.ws.wsdl.wsdl11.SimpleWsdl11Definition;
import org.springframework.ws.wsdl.wsdl11.Wsdl11Definition;

@Configuration
public class WebServiceConfig {

    @Bean
    public ServletRegistrationBean<MessageDispatcherServlet> messageDispatcherServlet(ApplicationContext context) {
        MessageDispatcherServlet servlet = new MessageDispatcherServlet();
        servlet.setApplicationContext(context);
        servlet.setTransformWsdlLocations(true);
        return new ServletRegistrationBean<>(servlet, "/ws/*");
    }

    @Bean
    public Jaxb2Marshaller marshaller() {
        Jaxb2Marshaller marshaller = new Jaxb2Marshaller();
        marshaller.setClassesToBeBound(
                CreateOrderRequest.class, CreateOrderResponse.class,
                OrderStatusRequest.class, OrderStatusResponse.class,
                CancelOrderRequest.class, CancelOrderResponse.class);
        return marshaller;
    }

    @Bean(name = "cms")
    public Wsdl11Definition cmsWsdlDefinition() {
        return new SimpleWsdl11Definition(new ClassPathResource("wsdl/cms.wsdl"));
    }
}
