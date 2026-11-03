package com.mywallet.transaction.client;

import java.math.BigDecimal;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywallet.transaction.exception.BankUnavailableException;

/**
 * Débit et crédit idempotents sur reference. Un 401 invalide le cache et réessaie une fois.
 */
@Component
public class BankClient {

	private final RestClient bank;
	private final BankTokenProvider tokens;

	public BankClient(
			RemoteHttp remoteHttp,
			BankTokenProvider tokens,
			@Value("${app.bank.base-url}") String baseUrl) {
		this.bank = remoteHttp.client(baseUrl);
		this.tokens = tokens;
	}

	public RemoteOutcome debit(String reference, String accountNumber, BigDecimal amount) {
		return invoke(() -> post("/bank/operations/debit", reference, accountNumber, amount));
	}

	public RemoteOutcome credit(String reference, String accountNumber, BigDecimal amount) {
		return invoke(() -> post("/bank/operations/credit", reference, accountNumber, amount));
	}

	private RemoteOutcome invoke(Supplier<Attempt> call) {
		try {
			Attempt first = call.get();
			if (!first.retry()) {
				return first.outcome();
			}
			tokens.invalidate();
			Attempt second = call.get();
			if (second.retry()) {
				return RemoteOutcome.unknown();
			}
			return second.outcome();
		}
		catch (BankUnavailableException | RestClientException exception) {
			return RemoteOutcome.unknown();
		}
	}

	private Attempt post(String path, String reference, String accountNumber, BigDecimal amount) {
		try {
			OperationBody body = bank.post()
					.uri(path)
					.accept(MediaType.APPLICATION_JSON)
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, authorization())
					.body(new OperationPayload(reference, accountNumber, amount.toPlainString()))
					.retrieve()
					.onStatus(status -> status.value() == 401, (request, response) -> {
						ResponseBodies.drain(response);
						throw new AttemptSignal(Attempt.unauthorized());
					})
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new AttemptSignal(Attempt.done(RemoteOutcome.unknown()));
					})
					.body(OperationBody.class);
			return Attempt.done(from(body));
		}
		catch (AttemptSignal signal) {
			return signal.attempt;
		}
		catch (RestClientException | BankUnavailableException exception) {
			return Attempt.done(RemoteOutcome.unknown());
		}
	}

	private RemoteOutcome from(OperationBody body) {
		if (body == null || body.status() == null) {
			return RemoteOutcome.unknown();
		}
		if ("SUCCESS".equals(body.status())) {
			return RemoteOutcome.success();
		}
		if ("REJECTED".equals(body.status())) {
			return RemoteOutcome.rejected(body.rejectionReason());
		}
		return RemoteOutcome.unknown();
	}

	private String authorization() {
		String token = tokens.token();
		if (token.indexOf('\r') >= 0 || token.indexOf('\n') >= 0) {
			throw new BankUnavailableException();
		}
		return "Bearer " + token;
	}

	private record OperationPayload(String reference, String accountNumber, String amount) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record OperationBody(String status, String rejectionReason) {
	}

	private record Attempt(RemoteOutcome outcome, boolean retry) {

		private static Attempt done(RemoteOutcome outcome) {
			return new Attempt(outcome, false);
		}

		private static Attempt unauthorized() {
			return new Attempt(RemoteOutcome.unknown(), true);
		}

	}

	private static final class AttemptSignal extends RuntimeException {

		private final Attempt attempt;

		private AttemptSignal(Attempt attempt) {
			this.attempt = attempt;
		}

	}

}
