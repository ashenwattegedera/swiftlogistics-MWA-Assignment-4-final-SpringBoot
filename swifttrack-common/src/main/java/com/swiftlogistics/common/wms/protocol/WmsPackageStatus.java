package com.swiftlogistics.common.wms.protocol;

/**
 * Package lifecycle tracked by the WMS.
 */
public enum WmsPackageStatus {
    RECEIVED,
    PICKED,
    PACKED,
    LOADED,
    OUT_FOR_DELIVERY,
    DELIVERED,
    FAILED
}
