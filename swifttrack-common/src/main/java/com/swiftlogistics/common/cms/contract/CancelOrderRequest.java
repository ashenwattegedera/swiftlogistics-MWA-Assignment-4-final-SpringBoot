package com.swiftlogistics.common.cms.contract;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "cancelOrderRequest", namespace = "http://www.swiftlogistics.lk/cms")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "cancelOrderRequest", namespace = "http://www.swiftlogistics.lk/cms")
public class CancelOrderRequest {

    @XmlElement(required = true)
    private String cmsOrderId;

    @XmlElement
    private String reason;

    public CancelOrderRequest() {
    }

    public CancelOrderRequest(String cmsOrderId, String reason) {
        this.cmsOrderId = cmsOrderId;
        this.reason = reason;
    }

    public String getCmsOrderId() {
        return cmsOrderId;
    }

    public void setCmsOrderId(String cmsOrderId) {
        this.cmsOrderId = cmsOrderId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
