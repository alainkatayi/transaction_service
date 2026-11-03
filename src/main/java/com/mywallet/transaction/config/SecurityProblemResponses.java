package com.mywallet.transaction.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import com.mywallet.transaction.exception.ProblemDetails;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import tools.jackson.databind.json.JsonMapper;

@Component
public class SecurityProblemResponses implements AuthenticationEntryPoint, AccessDeniedHandler {

	private final JsonMapper jsonMapper;

	public SecurityProblemResponses(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	@Override
	public void commence(
			HttpServletRequest request,
			HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		write(request, response, HttpStatus.UNAUTHORIZED, "Non autorisé", "Authentification requise.", "UNAUTHENTICATED");
	}

	@Override
	public void handle(
			HttpServletRequest request,
			HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		write(request, response, HttpStatus.FORBIDDEN, "Interdit", "Accès refusé.", "FORBIDDEN");
	}

	private void write(
			HttpServletRequest request,
			HttpServletResponse response,
			HttpStatus status,
			String title,
			String detail,
			String code) throws IOException {
		if (response.isCommitted()) {
			return;
		}
		ProblemDetail problem = ProblemDetails.of(status, title, detail, code, request);
		response.setStatus(problem.getStatus());
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
		if (status == HttpStatus.UNAUTHORIZED) {
			response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		}
		jsonMapper.writeValue(response.getOutputStream(), problem);
	}

}
