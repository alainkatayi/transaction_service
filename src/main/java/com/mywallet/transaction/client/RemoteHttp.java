package com.mywallet.transaction.client;

import java.net.http.HttpClient;
import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class RemoteHttp {

	private final Duration connectTimeout;
	private final Duration readTimeout;

	public RemoteHttp(
			@Value("${app.http.connect-timeout}") Duration connectTimeout,
			@Value("${app.http.read-timeout}") Duration readTimeout) {
		this.connectTimeout = connectTimeout;
		this.readTimeout = readTimeout;
	}

	public RestClient client(String baseUrl) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(connectTimeout)
				.build();
		JdkClientHttpRequestFactory factory = new JdkClientHttpRequestFactory(httpClient);
		factory.setReadTimeout(readTimeout);
		RestClient.Builder builder = RestClient.builder().requestFactory(factory);
		if (baseUrl != null && !baseUrl.isBlank()) {
			builder.baseUrl(baseUrl);
		}
		return builder.build();
	}

}
