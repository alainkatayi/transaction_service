package com.mywallet.transaction.client;

import java.io.IOException;
import java.io.InputStream;

import org.springframework.http.client.ClientHttpResponse;

final class ResponseBodies {

	private ResponseBodies() {
	}

	static void drain(ClientHttpResponse response) {
		try (InputStream body = response.getBody()) {
			body.readAllBytes();
		}
		catch (IOException exception) {
			// le corps peut contenir un jeton : on ne le journalise pas
		}
	}

	static byte[] read(ClientHttpResponse response) {
		try (InputStream body = response.getBody()) {
			return body.readAllBytes();
		}
		catch (IOException exception) {
			return new byte[0];
		}
	}

}
