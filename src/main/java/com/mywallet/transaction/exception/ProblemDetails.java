package com.mywallet.transaction.exception;

import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

import jakarta.servlet.http.HttpServletRequest;

public final class ProblemDetails {

	private ProblemDetails() {
	}

	public static ProblemDetail of(
			HttpStatus status,
			String title,
			String detail,
			String code,
			HttpServletRequest request) {
		ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
		problem.setTitle(title);
		String path = request.getRequestURI();
		if (path == null || path.isBlank()) {
			path = "/";
		}
		problem.setInstance(URI.create(path));
		problem.setProperty("code", code);
		return problem;
	}

}
