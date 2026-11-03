package com.mywallet.transaction.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;

import com.mywallet.transaction.exception.UnauthenticatedException;

@Component
public class AccessToken {

	public String bearer() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		if (authentication instanceof JwtAuthenticationToken jwtAuthentication) {
			String value = jwtAuthentication.getToken().getTokenValue();
			if (value != null && !value.isBlank()) {
				return value;
			}
		}
		throw new UnauthenticatedException();
	}

}
