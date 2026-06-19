package com.wastecoder.shopflow.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * Makes the gateway an OAuth2 resource server (reactive): every proxied business route requires a valid
 * Keycloak JWT, while the gateway's own operational endpoints ({@code /actuator/**}) stay open. The bearer
 * token is forwarded unchanged to the downstream services (Spring Cloud Gateway relays the Authorization
 * header by default), which re-validate it and enforce the per-role rules — so authorization lives in the
 * services and the gateway only authenticates at the edge.
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

	@Bean
	SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
		http
				.authorizeExchange(exchanges -> exchanges
						.pathMatchers("/actuator/**").permitAll()
						.anyExchange().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.csrf(ServerHttpSecurity.CsrfSpec::disable);
		return http.build();
	}
}
