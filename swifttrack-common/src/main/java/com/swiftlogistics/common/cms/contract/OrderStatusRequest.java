package com.swiftlogistics.common.cms.contract;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "orderStatusRequest", namespace = "http://www.swiftlogistics.lk/cms")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "orderStatusRequest", namespace = "http://www.swiftlogistics.lk/cms")
public class OrderStatusRequest {

    @XmlElement(required = true)
    private String cmsOrderId;

    public OrderStatusRequest() {
    }

    public OrderStatusRequest(String cmsOrderId) {
        this.cmsOrderId = cmsOrderId;
    }

    public String getCmsOrderId() {
        return cmsOrderId;
    }

    public void setCmsOrderId(String cmsOrderId) {
        this.cmsOrderId = cmsOrderId;
    }
}
