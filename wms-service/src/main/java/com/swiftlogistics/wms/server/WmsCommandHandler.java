package com.swiftlogistics.wms.server;

import com.swiftlogistics.common.wms.protocol.WmsCommands;
import com.swiftlogistics.common.wms.protocol.WmsFrame;
import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import com.swiftlogistics.wms.domain.Package;
import com.swiftlogistics.wms.service.PackageService;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Dispatches a decoded {@link WmsFrame} to the appropriate package operation and translates
 * the result back into wire frames. Status changes also produce a broadcast event so that
 * every connected client (the integration middleware) receives the real-time update.
 */
@Component
public class WmsCommandHandler {

    public static final String EVENT_STATUS_UPDATED = "STATUS_UPDATED";

    private final PackageService packageService;

    public WmsCommandHandler(PackageService packageService) {
        this.packageService = packageService;
    }

    public HandleResult handle(WmsFrame request) {
        String requestId = request.requestId();
        Map<String, Object> payload = request.payload() == null ? Map.of() : request.payload();

        return switch (request.command()) {
            case WmsCommands.ADD_PACKAGE -> handleAdd(requestId, payload);
            case WmsCommands.UPDATE_STATUS -> handleUpdate(requestId, payload);
            case WmsCommands.GET_PACKAGE -> handleGet(requestId, payload);
            case WmsCommands.LIST_PACKAGES -> handleList(requestId);
            default -> HandleResult.respond(WmsFrame.failure(requestId, request.command(), "Unknown command"));
        };
    }

    private HandleResult handleAdd(String requestId, Map<String, Object> payload) {
        Package pkg = packageService.addPackage(
                str(payload.get("packageId")),
                str(payload.get("orderId")),
                str(payload.get("recipientName")),
                str(payload.get("address")));
        return HandleResult.respond(WmsFrame.success(requestId, WmsCommands.ADD_PACKAGE, toMap(pkg)));
    }

    private HandleResult handleUpdate(String requestId, Map<String, Object> payload) {
        String packageId = str(payload.get("packageId"));
        WmsPackageStatus status = WmsPackageStatus.valueOf(str(payload.get("status")));
        String reason = str(payload.get("reason"));

        Optional<Package> updated = packageService.updateStatus(packageId, status, reason);
        if (updated.isEmpty()) {
            return HandleResult.respond(WmsFrame.failure(requestId, WmsCommands.UPDATE_STATUS, "Unknown package " + packageId));
        }
        Map<String, Object> map = toMap(updated.get());
        WmsFrame event = WmsFrame.event(EVENT_STATUS_UPDATED, map);
        return HandleResult.respondAndBroadcast(
                WmsFrame.success(requestId, WmsCommands.UPDATE_STATUS, map), event);
    }

    private HandleResult handleGet(String requestId, Map<String, Object> payload) {
        String packageId = str(payload.get("packageId"));
        return packageService.get(packageId)
                .map(pkg -> HandleResult.respond(WmsFrame.success(requestId, WmsCommands.GET_PACKAGE, toMap(pkg))))
                .orElseGet(() -> HandleResult.respond(WmsFrame.failure(requestId, WmsCommands.GET_PACKAGE, "Unknown package " + packageId)));
    }

    private HandleResult handleList(String requestId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("packages", packageService.all().stream().map(this::toMap).toList());
        return HandleResult.respond(WmsFrame.success(requestId, WmsCommands.LIST_PACKAGES, result));
    }

    private Map<String, Object> toMap(Package pkg) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("packageId", pkg.getPackageId());
        map.put("orderId", pkg.getOrderId());
        map.put("recipientName", pkg.getRecipientName());
        map.put("address", pkg.getAddress());
        map.put("status", pkg.getStatus().name());
        map.put("updatedAt", pkg.getUpdatedAt().toString());
        return map;
    }

    private String str(Object value) {
        return value == null ? null : value.toString();
    }
}
