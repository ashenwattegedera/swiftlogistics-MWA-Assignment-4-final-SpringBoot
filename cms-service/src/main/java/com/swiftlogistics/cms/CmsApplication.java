package com.swiftlogistics.cms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.webservices.WebServicesAutoConfiguration;

/**
 * The legacy CMS is deliberately configured fully by hand (see {@link com.swiftlogistics.cms.config.WebServiceConfig})
 * to mirror a real SOAP endpoint. Spring Boot's web-services auto-configuration is excluded so the
 * contract-first WSDL and JAXB marshaller are the single source of truth.
 */
@SpringBootApplication(exclude = WebServicesAutoConfiguration.class)
public class CmsApplication {
    public static void main(String[] args) {
        SpringApplication.run(CmsApplication.class, args);
    }
}
