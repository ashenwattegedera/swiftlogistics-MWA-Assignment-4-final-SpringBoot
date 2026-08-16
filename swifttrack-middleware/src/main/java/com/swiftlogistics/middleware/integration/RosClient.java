package com.swiftlogistics.middleware.integration;

import com.swiftlogistics.common.ros.contract.RouteOptimizeRequest;
import com.swiftlogistics.common.ros.contract.RouteOptimizeResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * REST client for the third-party ROS. Uses the JDK's synchronous HTTP client behind a
 * Spring RestClient; in production this would be wrapped with a circuit breaker and timeout.
 * The RestClient is built lazily so tests can bind a {@code MockRestServiceServer} to the
 * shared {@link RestClient.Builder} before the first request.
 */
@Component
public class RosClient {

    private final RestClient.Builder builder;
    private final String url;
    private volatile RestClient restClient;

    public RosClient(RestClient.Builder builder,
                     @Value("${swifttrack.ros.url:http://localhost:8082}") String url) {
        this.builder = builder;
        this.url = url;
    }

    public RouteOptimizeResponse optimize(RouteOptimizeRequest request) {
        return client().post()
                .uri("/api/routes/optimize")
                .body(request)
                .retrieve()
                .body(RouteOptimizeResponse.class);
    }

    private RestClient client() {
        RestClient client = restClient;
        if (client == null) {
            synchronized (this) {
                client = restClient;
                if (client == null) {
                    client = builder.baseUrl(url).build();
                    restClient = client;
                }
            }
        }
        return client;
    }
}
