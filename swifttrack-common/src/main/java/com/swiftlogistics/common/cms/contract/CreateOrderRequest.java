package com.swiftlogistics.common.cms.contract;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;
import jakarta.xml.bind.annotation.XmlType;

@XmlRootElement(name = "createOrderRequest", namespace = "http://www.swiftlogistics.lk/cms")
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "createOrderRequest", namespace = "http://www.swiftlogistics.lk/cms")
public class CreateOrderRequest {

    @XmlElement(required = true)
    private String clientId;

    @XmlElement(required = true)
    private String clientReference;

    @XmlElement(required = true)
    private Double amount;

    @XmlElement
    private String notes;

    public CreateOrderRequest() {
    }

    public CreateOrderRequest(String clientId, String clientReference, Double amount, String notes) {
        this.clientId = clientId;
        this.clientReference = clientReference;
        this.amount = amount;
        this.notes = notes;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientReference() {
        return clientReference;
    }

    public void setClientReference(String clientReference) {
        this.clientReference = clientReference;
    }

    public Double getAmount() {
        return amount;
    }

    public void setAmount(Double amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
