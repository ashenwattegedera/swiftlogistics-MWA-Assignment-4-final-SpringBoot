package com.swiftlogistics.wms.domain;

import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;

import java.time.Instant;

public class Package {

    private String packageId;
    private String orderId;
    private String recipientName;
    private String address;
    private WmsPackageStatus status;
    private Instant updatedAt;

    public Package() {
    }

    public Package(String packageId, String orderId, String recipientName, String address,
                   WmsPackageStatus status, Instant updatedAt) {
        this.packageId = packageId;
        this.orderId = orderId;
        this.recipientName = recipientName;
        this.address = address;
        this.status = status;
        this.updatedAt = updatedAt;
    }

    public String getPackageId() {
        return packageId;
    }

    public void setPackageId(String packageId) {
        this.packageId = packageId;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getRecipientName() {
        return recipientName;
    }

    public void setRecipientName(String recipientName) {
        this.recipientName = recipientName;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public WmsPackageStatus getStatus() {
        return status;
    }

    public void setStatus(WmsPackageStatus status) {
        this.status = status;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
