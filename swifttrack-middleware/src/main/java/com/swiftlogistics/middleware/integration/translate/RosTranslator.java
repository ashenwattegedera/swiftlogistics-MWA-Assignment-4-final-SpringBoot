package com.swiftlogistics.middleware.integration.translate;

import com.swiftlogistics.common.model.CanonicalOrder;
import com.swiftlogistics.common.ros.contract.RouteStop;
import org.springframework.stereotype.Component;

/**
 * Message Translator: canonical model &lt;-&gt; ROS REST/JSON contract.
 */
@Component
public class RosTranslator {

    public RouteStop toRouteStop(CanonicalOrder order, int sequence) {
        return new RouteStop(
                order.orderId(),
                sequence,
                order.orderId(),
                order.recipient().name(),
                order.address());
    }
}
