package com.swiftlogistics.common.wms.protocol;

/**
 * Commands understood by the proprietary WMS TCP/IP protocol.
 */
public final class WmsCommands {

    public static final String ADD_PACKAGE = "ADD_PACKAGE";
    public static final String UPDATE_STATUS = "UPDATE_STATUS";
    public static final String GET_PACKAGE = "GET_PACKAGE";
    public static final String LIST_PACKAGES = "LIST_PACKAGES";

    private WmsCommands() {
    }
}
