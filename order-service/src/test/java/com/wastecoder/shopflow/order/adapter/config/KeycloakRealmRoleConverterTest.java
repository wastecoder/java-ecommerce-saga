package com.wastecoder.shopflow.order.adapter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KeycloakRealmRoleConverterTest {

	private final KeycloakRealmRoleConverter converter = new KeycloakRealmRoleConverter();

	private static Jwt jwtWithClaims(Map<String, Object> claims) {
		Jwt.Builder builder = Jwt.withTokenValue("token").header("alg", "none").issuedAt(Instant.EPOCH);
		claims.forEach(builder::claim);
		return builder.build();
	}

	@Test
	@DisplayName("Given a token with realm_access.roles, when converting, then it maps each role to a ROLE_ authority")
	void convert_mapsRealmRolesToAuthorities() {
		Jwt jwt = jwtWithClaims(Map.of("realm_access", Map.of("roles", List.of("CUSTOMER", "ADMIN"))));

		assertThat(converter.convert(jwt))
				.extracting(GrantedAuthority::getAuthority)
				.containsExactlyInAnyOrder("ROLE_CUSTOMER", "ROLE_ADMIN");
	}

	@Test
	@DisplayName("Given a token without the realm_access claim, when converting, then it returns no authorities")
	void convert_withoutRealmAccess_returnsEmpty() {
		Jwt jwt = jwtWithClaims(Map.of("scope", "openid"));

		assertThat(converter.convert(jwt)).isEmpty();
	}

	@Test
	@DisplayName("Given a realm_access claim without a roles list, when converting, then it returns no authorities")
	void convert_withoutRolesList_returnsEmpty() {
		Jwt jwt = jwtWithClaims(Map.of("realm_access", Map.of("other", "value")));

		assertThat(converter.convert(jwt)).isEmpty();
	}
}
