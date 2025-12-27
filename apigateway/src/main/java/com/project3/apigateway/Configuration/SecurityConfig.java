package com.project3.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.reactive.CorsConfigurationSource;
import org.springframework.web.server.ServerWebExchange;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableReactiveMethodSecurity
@Slf4j
public class SecurityConfig {
    
    @org.springframework.beans.factory.annotation.Value("${cors.allowed-origins:http://localhost:5173}")
    private String allowedOrigins;
    
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8180/realms/project3}")
    private String issuerUri;
    
    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.client-id:project3}")
    private String clientId;
    
    @Bean
    public ReactiveJwtDecoder jwtDecoder() {
        return NimbusReactiveJwtDecoder.withIssuerLocation(issuerUri).build();
    }
    
    @Bean
    public SecurityWebFilterChain filterChain(ServerHttpSecurity http) {
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(exchange -> exchange
                        // Public endpoints - không cần authentication
                        .pathMatchers(
                                "/api/users/login",
                                "/api/users/register",
                                "/api/users/*/verify-email",
                                "/api/users/oauth/**",
                                "/api/v1/public/**",
                                "/actuator/**",
                                "/ws/**" // WebSocket endpoints - allow all (authentication handled by service)
                        ).permitAll()
                        // Users: self profile endpoints - tất cả user đã đăng nhập
                        .pathMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        // Users: admin management endpoints - RESTAURANT_MANAGER và ADMIN
                        .pathMatchers("/api/users/**").hasAnyRole("RESTAURANT_MANAGER", "ADMIN")
                        // Menu: read public, write requires restaurant staff/manager/admin
                        .pathMatchers(HttpMethod.GET, "/api/restaurant/menu/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/menu/**").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/restaurant/menu/**").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/restaurant/menu/**").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/restaurant/menu/**").hasAnyRole("RESTAURANT_MANAGER", "ADMIN")
                        // Inventory: warehouse staff, restaurant manager, admin only
                        .pathMatchers("/api/inventory/**").hasAnyRole("WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        // Orders: customer can create/view own orders, staff/manager/admin can manage all
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/create").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/restaurant/order", "/api/restaurant/order/**").hasAnyRole("CUSTOMER", "STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/restaurant/order/*/status").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/cancel").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/split-bill").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/start-cooking").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/mark-ready").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/start-delivering").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/*/complete").hasAnyRole("STAFF", "KITCHEN_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/restaurant/order/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/restaurant/order/**").hasAnyRole("RESTAURANT_MANAGER", "ADMIN")
                        // Cloudinary: signature for authenticated users
                        .pathMatchers("/api/cloudinary/**").authenticated()
                        // All other endpoints require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt
                                .jwtDecoder(jwtDecoder())
                                .jwtAuthenticationConverter(jwtAuthenticationConverter())
                        )
                );
        return http.build();
    }
    
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        // Support multiple origins separated by comma
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
        corsConfig.setAllowedOrigins(origins.isEmpty() ? List.of("http://localhost:5173") : origins);
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);
        
        // Custom CorsConfigurationSource that excludes WebSocket paths
        return new CorsConfigurationSource() {
            private final UrlBasedCorsConfigurationSource delegate = new UrlBasedCorsConfigurationSource();
            
            {
                delegate.registerCorsConfiguration("/**", corsConfig);
            }
            
            @Override
            public CorsConfiguration getCorsConfiguration(ServerWebExchange exchange) {
                String path = exchange.getRequest().getURI().getPath();
                // Don't add CORS headers for WebSocket - let Notification Service handle it
                if (path != null && path.startsWith("/ws/")) {
                    return null; // No CORS config - Notification Service will handle it
                }
                return delegate.getCorsConfiguration(exchange);
            }
        };
    }
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        return new CorsWebFilter(corsConfigurationSource());
    }

    @Bean
    public Converter<Jwt, Mono<AbstractAuthenticationToken>> jwtAuthenticationConverter() {
        JwtAuthenticationConverter baseConverter = new JwtAuthenticationConverter();
        baseConverter.setJwtGrantedAuthoritiesConverter(jwt -> extractAuthorities(jwt));
        return new ReactiveJwtAuthenticationConverterAdapter(baseConverter);
    }

    private Collection<GrantedAuthority> extractAuthorities(Jwt jwt) {
        // realm roles
        Collection<String> realmRoles = List.of();
        Map<String, Object> realmAccess = jwt.getClaim("realm_access");
        if (realmAccess != null) {
            Object roles = realmAccess.get("roles");
            if (roles instanceof Collection<?>) {
                realmRoles = ((Collection<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
            }
        }
        // client roles (e.g., project3)
        Collection<String> clientRoles = List.of();
        Map<String, Object> resourceAccess = jwt.getClaim("resource_access");
        if (resourceAccess != null && resourceAccess instanceof Map) {
            // Try to get roles from specific client (project3)
            Object clientAccess = resourceAccess.get(clientId);
            if (clientAccess instanceof Map) {
                Object roles = ((Map<?, ?>) clientAccess).get("roles");
                if (roles instanceof Collection<?>) {
                    clientRoles = ((Collection<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
                }
            }
            // Fallback: if not found in specific client, try first available client
            if (clientRoles.isEmpty()) {
                for (Object clientObj : resourceAccess.values()) {
                    if (clientObj instanceof Map) {
                        Object roles = ((Map<?, ?>) clientObj).get("roles");
                        if (roles instanceof Collection<?>) {
                            clientRoles = ((Collection<?>) roles).stream().map(Object::toString).collect(Collectors.toList());
                            break;
                        }
                    }
                }
            }
        }
        
        Collection<GrantedAuthority> authorities = java.util.stream.Stream.concat(realmRoles.stream(), clientRoles.stream())
                .distinct()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
        
        // Log at INFO level for debugging authorization issues
        log.info("Extracted authorities for user: realmRoles={}, clientRoles={}, authorities={}", 
                realmRoles, clientRoles, authorities.stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()));
        
        return authorities;
    }
}
