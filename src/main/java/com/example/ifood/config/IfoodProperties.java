package com.example.ifood.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "ifood")
public class IfoodProperties {

	private boolean mockEnabled = true;
	private boolean pollingEnabled = false;
	private long pollingIntervalMs = 30_000L;

	private String clientId;
	private String clientSecret;
	private String refreshToken;
	private String merchantId;

	private String apiBaseUrl = "https://merchant-api.ifood.com.br";

	public String eventsBaseUrl() {
		return apiBaseUrl + "/events/v1.0";
	}

	public String orderBaseUrl() {
		return apiBaseUrl + "/order/v1.0";
	}

	public String authUrl() {
		return apiBaseUrl + "/authentication/v1.0/oauth/token";
	}

}
