package com.mywallet.transaction.client;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.mywallet.transaction.exception.RecipientNotFoundException;
import com.mywallet.transaction.exception.ServiceUnavailableException;

import tools.jackson.databind.json.JsonMapper;

@Component
public class UserClient {

	private static final String KEY_HEADER = "X-Internal-Key";

	private final RestClient users;
	private final String apiKey;
	private final JsonMapper jsonMapper;

	public UserClient(
			RemoteHttp remoteHttp,
			JsonMapper jsonMapper,
			@Value("${app.user-service.base-url}") String baseUrl,
			@Value("${app.internal.api-key}") String apiKey) {
		this.users = remoteHttp.client(baseUrl);
		this.jsonMapper = jsonMapper;
		this.apiKey = apiKey;
	}

	public UserView byPhone(String phone) {
		try {
			UserBody body = users.get()
					// {phone} force l’encodage du « + » en %2B. queryParam brut laisse « + » = espace.
					.uri("/internal/users/by-phone?phone={phone}", phone)
					.accept(MediaType.APPLICATION_JSON)
					.header(KEY_HEADER, apiKey)
					.retrieve()
					.onStatus(status -> status.value() == 404, (request, response) -> {
						String code = code(response);
						if ("USER_NOT_FOUND".equals(code)) {
							throw new RecipientNotFoundException();
						}
						throw new ServiceUnavailableException();
					})
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new ServiceUnavailableException();
					})
					.body(UserBody.class);
			return view(body);
		}
		catch (RecipientNotFoundException | ServiceUnavailableException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException();
		}
	}

	public UserView byId(UUID userId) {
		try {
			UserBody body = users.get()
					.uri("/internal/users/{userId}", userId)
					.accept(MediaType.APPLICATION_JSON)
					.header(KEY_HEADER, apiKey)
					.retrieve()
					.onStatus(status -> status.value() == 404, (request, response) -> {
						ResponseBodies.drain(response);
						throw new RecipientNotFoundException();
					})
					.onStatus(status -> status.isError(), (request, response) -> {
						ResponseBodies.drain(response);
						throw new ServiceUnavailableException();
					})
					.body(UserBody.class);
			return view(body);
		}
		catch (RecipientNotFoundException | ServiceUnavailableException exception) {
			throw exception;
		}
		catch (RestClientException exception) {
			throw new ServiceUnavailableException();
		}
	}

	private static UserView view(UserBody body) {
		if (body == null || body.id() == null || body.phone() == null || body.firstName() == null || body.lastName() == null) {
			throw new ServiceUnavailableException();
		}
		return new UserView(body.id(), body.firstName().trim(), body.lastName().trim(), body.phone().trim(), body.status());
	}

	private String code(org.springframework.http.client.ClientHttpResponse response) {
		byte[] body = ResponseBodies.read(response);
		if (body.length == 0 || body.length > 4096) {
			return null;
		}
		try {
			ProblemCode parsed = jsonMapper.readValue(body, ProblemCode.class);
			return parsed.code();
		}
		catch (RuntimeException exception) {
			return null;
		}
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record UserBody(UUID id, String firstName, String lastName, String phone, String status) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	private record ProblemCode(String code) {
	}

	public record UserView(UUID id, String firstName, String lastName, String phone, String status) {
	}

}
