package com.swiftlogistics.common.cms.contract;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "createOrderResponse", namespace = "http://www.swiftlogistics.lk/cms")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createOrderResponse", namespace = "http://www.swiftlogistics.lk/cms")
public class CreateOrderResponse {

    @XmlElement(required = true)
    private String cmsOrderId;

    @XmlElement(required = true)
    private String status;

    @XmlElement
    private String message;

    public CreateOrderResponse() {
    }

    public CreateOrderResponse(String cmsOrderId, String status, String message) {
        this.cmsOrderId = cmsOrderId;
        this.status = status;
        this.message = message;
    }

    public String getCmsOrderId() {
        return cmsOrderId;
    }

    public void setCmsOrderId(String cmsOrderId) {
        this.cmsOrderId = cmsOrderId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
