package com.mywallet.transaction.client;

import java.time.Clock;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.mywallet.transaction.exception.BankUnavailableException;

/**
 * Cache mémoire du jeton client_credentials. Un seul renouvellement à la fois, 30 secondes avant expiration.
 * Le jeton et le secret ne sont pas journalisés.
 */
@Component
public class BankTokenProvider {

	private final RestClient tokenClient;
	private final String tokenUrl;
	private final String clientId;
	private final String clientSecret;
	private final String scopes;
	private final Clock clock;
	private final Object lock = new Object();
	private volatile Cached cached;

	public BankTokenProvider(
			RemoteHttp remoteHttp,
			@Value("${app.bank.token-url}") String tokenUrl,
			@Value("${app.bank.client-id}") String clientId,
			@Value("${app.bank.client-secret}") String clientSecret,
			@Value("${app.bank.scopes}") String scopes,
			Clock clock) {
		this.tokenClient = remoteHttp.client(null);
		this.tokenUrl = tokenUrl;
		this.clientId = clientId;
		this.clientSecret = clientSecret;
		this.scopes = scopes;
		this.clock = clock;
	}

	public String token() {
		Cached current = cached;
		if (usable(current)) {
			return current.token;
		}
		synchronized (lock) {
			current = cached;
			if (usable(current)) {
				return current.token;
			}
			Cached fresh = fetch();
			cached = fresh;
			return fresh.token;
		}
	}

	public void invalidate() {
		synchronized (lock) {
			cached = null;
		}
	}

	private boolean usable(Cached current) {
		return current != null && clock.instant().isBefore(current.refreshAt);
	}

	private Cached fetch() {
		MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
		form.add("grant_type", "client_credentials");
		form.add("client_id", clientId);
		form.add("client_secret", clientSecret);
		form.add("scope", scopes);
		TokenResponse body;
		try {
			body = tokenClient.post()
					.uri(tokenUrl)
					.contentType(MediaType.APPLICATION_FORM_URLENCODED)
					.accept(MediaType.APPLICATION_JSON)
					.body(form)
					.retrieve()
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new BankUnavailableException();
					})
					.body(TokenResponse.class);
		}
		catch (RestClientException exception) {
			throw new BankUnavailableException();
		}
		if (body == null || body.accessToken == null || body.accessToken.isBlank() || body.expiresIn < 1) {
			throw new BankUnavailableException();
		}
		Instant now = clock.instant();
		Instant expiry = now.plusSeconds(body.expiresIn);
		Instant refreshAt = expiry.minusSeconds(30);
		if (!refreshAt.isAfter(now)) {
			refreshAt = expiry;
		}
		return new Cached(body.accessToken, refreshAt);
	}

	private record Cached(String token, Instant refreshAt) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record TokenResponse(
			@JsonProperty("access_token") String accessToken,
			@JsonProperty("expires_in") long expiresIn) {
	}

}
