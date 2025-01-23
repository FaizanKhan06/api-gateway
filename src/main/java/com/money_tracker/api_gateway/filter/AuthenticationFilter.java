package com.money_tracker.api_gateway.filter;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;

@Component
public class AuthenticationFilter extends AbstractGatewayFilterFactory<AuthenticationFilter.Config> {

    @Autowired
    RouteValidator validator;

    public AuthenticationFilter() {
        super(Config.class);
    }

    public static class Config {
    }

    @Override
    public GatewayFilter apply(Config config) {
        return ((exchange, chain) -> {
            // for the uris NOT specified in the RouteValidator do the following steps
            if (validator.isSecured.test(exchange.getRequest())) {
                // check if the exchange request header contains the Authorization header
                if (!exchange.getRequest().getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
                    return Mono
                            .error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Missing Authorization Header"));
                    // throw new RuntimeException("Missing Authorization Header");
                }
                // take out the AUthorization header
                @SuppressWarnings("null")
                String authHeaderToken = exchange.getRequest().getHeaders().get(HttpHeaders.AUTHORIZATION).get(0);
                if (authHeaderToken != null && authHeaderToken.startsWith("Bearer")) {
                    // remove Bearer from front
                    authHeaderToken = authHeaderToken.substring(7);
                }
                try {
                    // Create the RestClient instance
                    RestClient restClient = RestClient.create();

                    // Make the request to the authentication service to validate the token
                    Boolean isValidToken = restClient.get()
                            .uri("http://localhost:8090/api/auth/validate/token?token=" + authHeaderToken)
                            .retrieve()
                            .body(Boolean.class); // Get the response body as a Boolean

                    // If the token is invalid (false), throw an exception to stop further
                    // processing
                    if (isValidToken == null || !isValidToken) {
                        // throw new RuntimeException("Invalid Token");
                        return Mono.error(
                                new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Token"));
                    }
                } catch (Exception e) {
                    // Handle any exceptions (e.g., network issues, invalid responses)
                    // throw new RuntimeException("Invalid Access!!: ");

                    return Mono.error(
                            new ResponseStatusException(HttpStatus.NOT_FOUND, "Invalid Access"));
                }

            }
            // for other uris simply chain the request.
            return chain.filter(exchange);
        });
    }
}