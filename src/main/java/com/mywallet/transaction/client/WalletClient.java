package com.mywallet.transaction.client;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywallet.transaction.exception.ServiceUnavailableException;
import com.mywallet.transaction.exception.WalletNotFoundException;

import tools.jackson.databind.json.JsonMapper;

@Component
public class WalletClient {

	private static final String KEY_HEADER = "X-Internal-Key";

	private final RestClient wallet;
	private final String apiKey;
	private final JsonMapper jsonMapper;

	public WalletClient(
			RemoteHttp remoteHttp,
			JsonMapper jsonMapper,
			@Value("${app.wallet.base-url}") String baseUrl,
			@Value("${app.internal.api-key}") String apiKey) {
		this.wallet = remoteHttp.client(baseUrl);
		this.jsonMapper = jsonMapper;
		this.apiKey = apiKey;
	}

	public WalletView getWallet(UUID userId) {
		try {
			WalletBody body = wallet.get()
					.uri("/internal/wallets/{userId}", userId)
					.accept(MediaType.APPLICATION_JSON)
					.header(KEY_HEADER, apiKey)
					.retrieve()
					.onStatus(status -> status.value() == 404, (request, response) -> {
						String code = code(response);
						if ("WALLET_NOT_FOUND".equals(code)) {
							throw new WalletNotFoundException();
						}
						throw new ServiceUnavailableException();
					})
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new ServiceUnavailableException();
					})
					.body(WalletBody.class);
			if (body == null || body.walletId() == null || body.status() == null || body.bankAccountNumber() == null) {
				throw new ServiceUnavailableException();
			}
			return new WalletView(body.walletId(), body.status().trim(), body.bankAccountNumber().trim());
		}
		catch (WalletNotFoundException | ServiceUnavailableException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException();
		}
	}

	public RemoteOutcome credit(UUID userId, String transactionRef, BigDecimal amount) {
		return movement("/internal/wallets/{userId}/credit", userId, new MovementPayload(transactionRef, amount.toPlainString()));
	}

	public RemoteOutcome debit(UUID userId, String transactionRef, BigDecimal amount) {
		return movement("/internal/wallets/{userId}/debit", userId, new MovementPayload(transactionRef, amount.toPlainString()));
	}

	public RemoteOutcome reverse(UUID userId, String transactionRef) {
		return movement("/internal/wallets/{userId}/reverse", userId, new ReversePayload(transactionRef));
	}

	public RemoteOutcome transferDebit(UUID userId, String transactionRef, BigDecimal amount) {
		return movement(
				"/internal/wallets/{userId}/transfer/debit",
				userId,
				new MovementPayload(transactionRef, amount.toPlainString()));
	}

	public RemoteOutcome transferCredit(UUID userId, String transactionRef, BigDecimal amount) {
		return movement(
				"/internal/wallets/{userId}/transfer/credit",
				userId,
				new MovementPayload(transactionRef, amount.toPlainString()));
	}

	public RemoteOutcome transferReverse(UUID userId, String transactionRef) {
		return movement("/internal/wallets/{userId}/transfer/reverse", userId, new ReversePayload(transactionRef));
	}

	private RemoteOutcome movement(String path, UUID userId, Object payload) {
		try {
			MovementBody body = wallet.post()
					.uri(path, userId)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.header(KEY_HEADER, apiKey)
					.body(payload)
					.retrieve()
					.onStatus(status -> status.isError(), (request, response) -> fail(response))
					.body(MovementBody.class);
			return fromMovement(body);
		}
		catch (CallSignal signal) {
			return signal.outcome;
		}
		catch (RestClientException exception) {
			return RemoteOutcome.unknown();
		}
	}

	private RemoteOutcome fromMovement(MovementBody body) {
		if (body == null || body.status() == null) {
			return RemoteOutcome.unknown();
		}
		if ("APPLIED".equals(body.status())) {
			return RemoteOutcome.success();
		}
		if ("REJECTED".equals(body.status())) {
			return RemoteOutcome.rejected(body.reason());
		}
		return RemoteOutcome.unknown();
	}

	private void fail(org.springframework.http.client.ClientHttpResponse response) {
		int status = status(response);
		String code = code(response);
		if (status == 404 && "WALLET_NOT_FOUND".equals(code)) {
			throw new CallSignal(RemoteOutcome.rejected("WALLET_NOT_FOUND"));
		}
		throw new CallSignal(RemoteOutcome.unknown());
	}

	private String code(org.springframework.http.client.ClientHttpResponse response) {
		byte[] body = ResponseBodies.read(response);
		if (body.length == 0 || body.length > 4096) {
			return null;
		}
		try {
			ProblemCode parsed = jsonMapper.readValue(body, ProblemCode.class);
			return RemoteOutcome.safe(parsed.code()).equals("REJECTED") ? null : parsed.code();
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	private static int status(org.springframework.http.client.ClientHttpResponse response) {
		try {
			return response.getStatusCode().value();
		}
		catch (java.io.IOException exception) {
			throw new CallSignal(RemoteOutcome.unknown());
		}
	}

	private record MovementPayload(String transactionRef, String amount) {
	}

	private record ReversePayload(String transactionRef) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record WalletBody(UUID walletId, String status, String bankAccountNumber) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record MovementBody(String status, String reason) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProblemCode(String code) {
	}

	private static final class CallSignal extends RuntimeException {

		private final RemoteOutcome outcome;

		private CallSignal(RemoteOutcome outcome) {
			this.outcome = outcome;
		}

	}

}
