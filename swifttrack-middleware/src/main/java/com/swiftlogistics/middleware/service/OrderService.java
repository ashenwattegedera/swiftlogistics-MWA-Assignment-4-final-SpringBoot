package com.swiftlogistics.middleware.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.swiftlogistics.common.events.OrderCreatedEvent;
import com.swiftlogistics.common.events.OrderStatusChangedEvent;
import com.swiftlogistics.common.model.Address;
import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.model.OrderItem;
import com.swiftlogistics.common.model.OrderStatus;
import com.swiftlogistics.common.model.Recipient;
import com.swiftlogistics.middleware.api.dto.OrderAcceptedResponse;
import com.swiftlogistics.middleware.api.dto.OrderResponse;
import com.swiftlogistics.middleware.api.dto.OrderSubmissionRequest;
import com.swiftlogistics.middleware.persistence.OrderEntity;
import com.swiftlogistics.middleware.persistence.OrderRepository;
import com.swiftlogistics.middleware.persistence.OutboxMessage;
import com.swiftlogistics.middleware.persistence.OutboxRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;

import static com.swiftlogistics.middleware.messaging.MessagingConstants.ORDER_EXCHANGE;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ROUTING_ORDER_CREATED;
import static com.swiftlogistics.middleware.messaging.MessagingConstants.ROUTING_ORDER_STATUS_CHANGED;

/**
 * Writes order state to the transactional store and enqueues the corresponding domain events
 * through the outbox, so state change and event publication are atomically consistent.
 */
@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OrderService(OrderRepository orderRepository, OutboxRepository outboxRepository,
                        ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public OrderAcceptedResponse createOrder(OrderSubmissionRequest request) {
        Instant now = Instant.now();
        String orderId = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        OrderEntity entity = new OrderEntity();
        entity.setOrderId(orderId);
        entity.setClientId(request.clientId());
        entity.setClientReference(request.clientReference());
        entity.setRecipientName(request.recipient().name());
        entity.setRecipientPhone(request.recipient().phone());
        entity.setAddressStreet(request.address().street());
        entity.setAddressCity(request.address().city());
        entity.setAddressPostalCode(request.address().postalCode());
        entity.setLatitude(request.address().latitude());
        entity.setLongitude(request.address().longitude());
        entity.setItemsJson(serializeItems(request.items()));
        entity.setStatus(OrderStatus.CREATED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        orderRepository.save(entity);

        OrderCreatedEvent event = new OrderCreatedEvent(
                orderId, request.clientId(), request.clientReference(),
                request.recipient(), request.address(), request.items(), now);
        enqueue(orderId, ORDER_EXCHANGE, ROUTING_ORDER_CREATED, event);

        return new OrderAcceptedResponse(orderId, OrderStatus.CREATED, now);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrder(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::toResponse)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listOrders() {
        return orderRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public CanonicalOrder getCanonical(String orderId) {
        return orderRepository.findById(orderId)
                .map(this::toCanonical)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> listByDriver(String driverId) {
        return orderRepository.findByDriverId(driverId).stream().map(this::toResponse).toList();
    }

    // --- saga-facing transitions (each atomic) -----------------------------------------

    @Transactional
    public void markCmsAccepted(String orderId, String cmsOrderId) {
        transition(orderId, OrderStatus.CMS_ACCEPTED, "order accepted by CMS",
                e -> e.setCmsOrderId(cmsOrderId));
    }

    @Transactional
    public void markWmsReceived(String orderId, String packageId) {
        transition(orderId, OrderStatus.WAREHOUSE_RECEIVED, "package received at warehouse",
                e -> e.setPackageId(packageId));
    }

    @Transactional
    public void markPicked(String orderId) {
        transition(orderId, OrderStatus.PICKED, "package picked", null);
    }

    @Transactional
    public void markPacked(String orderId) {
        transition(orderId, OrderStatus.PACKED, "package packed", null);
    }

    @Transactional
    public void markReadyForDelivery(String orderId) {
        transition(orderId, OrderStatus.READY_FOR_DELIVERY, "package loaded onto vehicle", null);
    }

    @Transactional
    public void markRouteAssigned(String orderId, String routeId, String driverId) {
        transition(orderId, OrderStatus.ROUTE_ASSIGNED, "route assigned to driver " + driverId,
                e -> {
                    e.setRouteId(routeId);
                    e.setDriverId(driverId);
                });
    }

    @Transactional
    public void markOutForDelivery(String orderId) {
        transition(orderId, OrderStatus.OUT_FOR_DELIVERY, "out for delivery", null);
    }

    @Transactional
    public void markDelivered(String orderId) {
        transition(orderId, OrderStatus.DELIVERED, "package delivered", null);
    }

    @Transactional
    public void markFailed(String orderId, String reason) {
        transition(orderId, OrderStatus.FAILED, reason == null ? "delivery failed" : reason,
                e -> e.setFailureReason(reason));
    }

    @Transactional
    public void markCancelled(String orderId, String reason) {
        transition(orderId, OrderStatus.CANCELLED, reason == null ? "cancelled" : reason,
                e -> e.setFailureReason(reason));
    }

    // --- internals ---------------------------------------------------------------------

    private void transition(String orderId, OrderStatus to, String message, Consumer<OrderEntity> mutator) {
        OrderEntity entity = orderRepository.findById(orderId)
                .orElseThrow(() -> new OrderNotFoundException(orderId));
        OrderStatus from = entity.getStatus();
        if (mutator != null) {
            mutator.accept(entity);
        }
        entity.setStatus(to);
        entity.setUpdatedAt(Instant.now());
        orderRepository.save(entity);

        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                orderId, from, to, message, Instant.now());
        enqueue(orderId, ORDER_EXCHANGE, ROUTING_ORDER_STATUS_CHANGED, event);
    }

    private void enqueue(String aggregateId, String exchange, String routingKey, Object event) {
        try {
            OutboxMessage outbox = new OutboxMessage(
                    aggregateId, exchange, routingKey,
                    event.getClass().getName(),
                    objectMapper.writeValueAsString(event),
                    Instant.now());
            outboxRepository.save(outbox);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise event " + event, e);
        }
    }

    private String serializeItems(List<OrderItem> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise items", e);
        }
    }

    private List<OrderItem> deserializeItems(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {
            });
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialise items", e);
        }
    }

    private CanonicalOrder toCanonical(OrderEntity e) {
        return new CanonicalOrder(
                e.getOrderId(),
                e.getClientId(),
                e.getClientReference(),
                new Recipient(e.getRecipientName(), e.getRecipientPhone()),
                new Address(e.getAddressStreet(), e.getAddressCity(), e.getAddressPostalCode(),
                        e.getLatitude(), e.getLongitude()),
                deserializeItems(e.getItemsJson()),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    private OrderResponse toResponse(OrderEntity e) {
        CanonicalOrder c = toCanonical(e);
        return new OrderResponse(
                c.orderId(),
                c.clientId(),
                c.clientReference(),
                c.recipient(),
                c.address(),
                c.items(),
                c.status(),
                e.getCmsOrderId(),
                e.getPackageId(),
                e.getRouteId(),
                e.getDriverId(),
                e.getFailureReason(),
                c.createdAt(),
                c.updatedAt());
    }
}
