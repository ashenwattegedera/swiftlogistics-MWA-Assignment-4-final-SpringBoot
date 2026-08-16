package com.swiftlogistics.wms.service;

import com.swiftlogistics.common.wms.protocol.WmsPackageStatus;
import com.swiftlogistics.wms.domain.Package;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * In-memory package store for the mock WMS. Packages move through the warehouse lifecycle
 * (RECEIVED -> PICKED -> PACKED -> LOADED -> ...) driven by status updates.
 */
@Service
public class PackageService {

    private final Map<String, Package> packages = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong(900);

    public Package addPackage(String packageId, String orderId, String recipientName, String address) {
        String id = packageId != null ? packageId : "PKG-" + sequence.incrementAndGet();
        Package pkg = new Package(id, orderId, recipientName, address, WmsPackageStatus.RECEIVED, Instant.now());
        packages.put(id, pkg);
        return pkg;
    }

    public Optional<Package> get(String packageId) {
        return Optional.ofNullable(packages.get(packageId));
    }

    public Optional<Package> updateStatus(String packageId, WmsPackageStatus status, String reason) {
        return get(packageId).map(pkg -> {
            pkg.setStatus(status);
            pkg.setUpdatedAt(Instant.now());
            return pkg;
        });
    }

    public Collection<Package> all() {
        return packages.values();
    }
}
