package com.wastecoder.shopflow.notification.adapter.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Makes notification-service an OAuth2 resource server: reading notifications ({@code GET /notifications/**})
 * requires a valid Keycloak JWT (any realm role); the operational (actuator) and documentation (Swagger)
 * endpoints stay open. The JWT is validated against the issuer configured in {@code application.yaml}
 * ({@code spring.security.oauth2.resourceserver.jwt.issuer-uri}).
 *
 * <p>Guarded by {@link ConditionalOnWebApplication}(SERVLET) so the bean is skipped in the non-web Spring
 * contexts of the end-to-end test harness, where no {@link HttpSecurity} exists.
 */
@Configuration
@EnableWebSecurity
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http
				.authorizeHttpRequests(authorize -> authorize
						.requestMatchers("/actuator/**").permitAll()
						.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
						.anyRequest().authenticated())
				.oauth2ResourceServer(oauth2 -> oauth2.jwt(Customizer.withDefaults()))
				.csrf(csrf -> csrf.disable())
				.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));
		return http.build();
	}
}
