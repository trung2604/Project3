package com.project3.apigateway.Configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableReactiveMethodSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.core.convert.converter.Converter;
import reactor.core.publisher.Mono;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableReactiveMethodSecurity
public class SecurityConfig {
    
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
                                "/api/v1/public/**"
                        ).permitAll()
                        // Users: self profile endpoints - tất cả user đã đăng nhập
                        .pathMatchers("/api/users/me", "/api/users/me/**").authenticated()
                        // Users: admin management endpoints - chỉ ADMIN
                        .pathMatchers("/api/users/**").hasRole("ADMIN")
                        // Menu: read public, write requires restaurant staff/manager/admin
                        .pathMatchers(HttpMethod.GET, "/api/restaurant/menu/**").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/menu/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/restaurant/menu/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/restaurant/menu/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/restaurant/menu/**").hasAnyRole("RESTAURANT_MANAGER", "ADMIN")
                        // Inventory: warehouse staff, restaurant manager, admin only
                        .pathMatchers("/api/inventory/**").hasAnyRole("WAREHOUSE_STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        // Orders: customer can create/view own orders, staff/manager/admin can manage all
                        .pathMatchers(HttpMethod.POST, "/api/restaurant/order/**").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/restaurant/order/**").hasAnyRole("CUSTOMER", "STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PUT, "/api/restaurant/order/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.PATCH, "/api/restaurant/order/**").hasAnyRole("STAFF", "RESTAURANT_MANAGER", "ADMIN")
                        .pathMatchers(HttpMethod.DELETE, "/api/restaurant/order/**").hasAnyRole("RESTAURANT_MANAGER", "ADMIN")
                        // Cloudinary: signature for authenticated users
                        .pathMatchers("/api/cloudinary/**").authenticated()
                        // All other endpoints require authentication
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter()))
                );
        return http.build();
    }
    
    @Bean
    public org.springframework.web.cors.reactive.CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        corsConfig.setAllowedOrigins(List.of("http://localhost:5173"));
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        corsConfig.setAllowedHeaders(List.of("*"));
        corsConfig.setAllowCredentials(true);
        corsConfig.setMaxAge(3600L);
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return source;
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
        if (resourceAccess != null) {
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
        return java.util.stream.Stream.concat(realmRoles.stream(), clientRoles.stream())
                .distinct()
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r.toUpperCase())
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toSet());
    }
}
