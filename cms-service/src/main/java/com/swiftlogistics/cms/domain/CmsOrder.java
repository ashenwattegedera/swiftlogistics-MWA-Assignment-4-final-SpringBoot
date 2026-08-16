package com.swiftlogistics.cms.domain;

import java.time.Instant;

public class CmsOrder {

    private String cmsOrderId;
    private String clientId;
    private String clientReference;
    private double amount;
    private String notes;
    private String status;
    private Instant createdAt;

    public CmsOrder() {
    }

    public CmsOrder(String cmsOrderId, String clientId, String clientReference, double amount,
                    String notes, String status, Instant createdAt) {
        this.cmsOrderId = cmsOrderId;
        this.clientId = clientId;
        this.clientReference = clientReference;
        this.amount = amount;
        this.notes = notes;
        this.status = status;
        this.createdAt = createdAt;
    }

    public String getCmsOrderId() {
        return cmsOrderId;
    }

    public void setCmsOrderId(String cmsOrderId) {
        this.cmsOrderId = cmsOrderId;
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

    public double getAmount() {
        return amount;
    }

    public void setAmount(double amount) {
        this.amount = amount;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
